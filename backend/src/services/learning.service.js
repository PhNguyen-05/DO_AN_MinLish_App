const db = require('../config/db');
const { createHttpError } = require('../utils/httpError');

const REVIEW_QUALITY = {
    AGAIN: 0,
    HARD: 1,
    GOOD: 2,
    EASY: 3
};

function normalizeLimit(value) {
    const parsed = Number.parseInt(value, 10);
    if (Number.isNaN(parsed)) return 20;
    return Math.min(Math.max(parsed, 1), 50);
}

function calculateSm2Progress(progress, quality) {
    const currentEase = Number(progress?.ease_factor || 2.5);
    const currentRepetitions = Number(progress?.repetitions || 0);
    const currentInterval = Number(progress?.interval_days || 0);

    let easeFactor = currentEase;
    let repetitions = currentRepetitions;
    let intervalDays = currentInterval;
    let nextReviewAt = new Date();

    if (quality === REVIEW_QUALITY.AGAIN) {
        easeFactor = Math.max(1.3, currentEase - 0.2);
        repetitions = 0;
        intervalDays = 0;
        nextReviewAt = new Date(Date.now() + 5 * 60 * 1000);
    } else if (quality === REVIEW_QUALITY.HARD) {
        easeFactor = Math.max(1.3, currentEase - 0.15);
        repetitions = currentRepetitions + 1;
        intervalDays = Math.max(1, Math.round(Math.max(currentInterval, 1) * 1.2));
        nextReviewAt = addDays(intervalDays);
    } else if (quality === REVIEW_QUALITY.GOOD) {
        repetitions = currentRepetitions + 1;
        if (currentRepetitions === 0) intervalDays = 1;
        else if (currentRepetitions === 1) intervalDays = 3;
        else intervalDays = Math.max(1, Math.round(currentInterval * easeFactor));
        nextReviewAt = addDays(intervalDays);
    } else if (quality === REVIEW_QUALITY.EASY) {
        easeFactor = currentEase + 0.15;
        repetitions = currentRepetitions + 1;
        if (currentRepetitions === 0) intervalDays = 3;
        else if (currentRepetitions === 1) intervalDays = 7;
        else intervalDays = Math.max(1, Math.round(currentInterval * easeFactor * 1.3));
        nextReviewAt = addDays(intervalDays);
    } else {
        throw createHttpError(400, 'Mức đánh giá không hợp lệ.');
    }

    return {
        easeFactor,
        repetitions,
        intervalDays,
        nextReviewAt
    };
}

function addDays(days) {
    return new Date(Date.now() + days * 24 * 60 * 60 * 1000);
}

async function getDailyPlan(userId) {
    const [[settings]] = await db.query(`
        SELECT daily_new_words_goal, daily_review_goal
        FROM user_settings
        WHERE user_id = ?
    `, [userId]);

    const [[dueReview]] = await db.query(`
        SELECT COUNT(*) AS count
        FROM card_progress
        WHERE user_id = ? AND next_review_at <= NOW()
    `, [userId]);

    const [[newWords]] = await db.query(`
        SELECT COUNT(*) AS count
        FROM cards c
        JOIN decks d ON c.deck_id = d.id
        WHERE (d.user_id IS NULL OR d.user_id = ?)
          AND NOT EXISTS (
              SELECT 1
              FROM card_progress cp
              WHERE cp.user_id = ? AND cp.card_id = c.id
          )
    `, [userId, userId]);

    const [[today]] = await db.query(`
        SELECT words_learned, words_reviewed
        FROM daily_progress
        WHERE user_id = ? AND study_date = CURDATE()
    `, [userId]);

    return {
        daily_new_words_goal: settings?.daily_new_words_goal || 20,
        daily_review_goal: settings?.daily_review_goal || 50,
        new_words_available: newWords.count,
        due_review_count: dueReview.count,
        words_learned_today: today?.words_learned || 0,
        words_reviewed_today: today?.words_reviewed || 0
    };
}

async function getDeckSummaries(userId) {
    const [rows] = await db.query(`
        SELECT d.id, d.title, d.description,
               COUNT(c.id) AS total_words,
               COALESCE(SUM(CASE WHEN cp.repetitions > 0 THEN 1 ELSE 0 END), 0) AS learned_words,
               COALESCE(SUM(CASE WHEN cp.id IS NULL THEN 1 ELSE 0 END), 0) AS new_words_count,
               COALESCE(SUM(CASE WHEN cp.next_review_at <= NOW() THEN 1 ELSE 0 END), 0) AS due_review_count,
               MAX(logs.last_studied_at) AS last_studied_at
        FROM decks d
        JOIN cards c ON c.deck_id = d.id
        LEFT JOIN card_progress cp
               ON cp.card_id = c.id AND cp.user_id = ?
        LEFT JOIN (
            SELECT card_id, MAX(created_at) AS last_studied_at
            FROM learning_logs
            WHERE user_id = ?
            GROUP BY card_id
        ) logs ON logs.card_id = c.id
        WHERE d.user_id IS NULL OR d.user_id = ?
        GROUP BY d.id, d.title, d.description
        ORDER BY
            CASE WHEN MAX(logs.last_studied_at) IS NULL THEN 1 ELSE 0 END,
            MAX(logs.last_studied_at) DESC,
            d.id ASC
    `, [userId, userId, userId]);

    const decks = rows.map(row => {
        const totalWords = Number(row.total_words || 0);
        const learnedWords = Number(row.learned_words || 0);
        return {
            id: row.id,
            title: row.title,
            description: row.description,
            total_words: totalWords,
            learned_words: learnedWords,
            new_words_count: Number(row.new_words_count || 0),
            due_review_count: Number(row.due_review_count || 0),
            last_studied_at: row.last_studied_at,
            is_completed: totalWords > 0 && learnedWords >= totalWords,
            is_in_progress: learnedWords > 0 && learnedWords < totalWords
        };
    });

    const continueDeck = decks.find(deck => deck.is_in_progress) || null;

    return {
        continue_deck: continueDeck,
        decks
    };
}

async function getLearningSession(userId, options = {}) {
    const mode = ['new', 'review', 'mixed'].includes(options.mode) ? options.mode : 'mixed';
    const limit = normalizeLimit(options.limit);
    const deckId = options.deckId ? Number.parseInt(options.deckId, 10) : null;

    let cards = [];
    if (mode === 'review' || mode === 'mixed') {
        cards = await getDueReviewCards(userId, limit, deckId);
    }

    if ((mode === 'new' || mode === 'mixed') && cards.length < limit) {
        const newCards = await getNewCards(userId, limit - cards.length, deckId);
        cards = cards.concat(newCards);
    }

    return {
        mode,
        count: cards.length,
        cards
    };
}

async function getDueReviewCards(userId, limit, deckId) {
    const params = [userId];
    let deckFilter = '';
    if (deckId) {
        deckFilter = 'AND c.deck_id = ?';
        params.push(deckId);
    }
    params.push(limit);

    const [rows] = await db.query(`
        SELECT c.id, c.deck_id, d.title AS deck_title, c.word, c.pronunciation, c.meaning,
               c.description_en, c.example, c.collocation, c.related_words, c.note,
               c.image_url, c.audio_url, cp.ease_factor, cp.repetitions,
               cp.interval_days, cp.next_review_at
        FROM card_progress cp
        JOIN cards c ON cp.card_id = c.id
        JOIN decks d ON c.deck_id = d.id
        WHERE cp.user_id = ? AND cp.next_review_at <= NOW()
          ${deckFilter}
        ORDER BY cp.next_review_at ASC, c.id ASC
        LIMIT ?
    `, params);

    return rows.map(row => toLearningCard(row, 'review'));
}

async function getNewCards(userId, limit, deckId) {
    const params = [userId, userId];
    let deckFilter = '';
    if (deckId) {
        deckFilter = 'AND c.deck_id = ?';
        params.push(deckId);
    }
    params.push(limit);

    const [rows] = await db.query(`
        SELECT c.id, c.deck_id, d.title AS deck_title, c.word, c.pronunciation, c.meaning,
               c.description_en, c.example, c.collocation, c.related_words, c.note,
               c.image_url, c.audio_url, NULL AS ease_factor, 0 AS repetitions,
               0 AS interval_days, NULL AS next_review_at
        FROM cards c
        JOIN decks d ON c.deck_id = d.id
        WHERE (d.user_id IS NULL OR d.user_id = ?)
          AND NOT EXISTS (
              SELECT 1
              FROM card_progress cp
              WHERE cp.user_id = ? AND cp.card_id = c.id
          )
          ${deckFilter}
        ORDER BY d.id ASC, c.id ASC
        LIMIT ?
    `, params);

    return rows.map(row => toLearningCard(row, 'new'));
}

function toLearningCard(row, type) {
    return {
        id: row.id,
        deck_id: row.deck_id,
        deck_title: row.deck_title,
        word: row.word,
        pronunciation: row.pronunciation,
        meaning: row.meaning,
        description_en: row.description_en,
        example: row.example,
        collocation: row.collocation,
        related_words: row.related_words,
        note: row.note,
        image_url: row.image_url,
        audio_url: row.audio_url,
        type,
        progress: {
            ease_factor: row.ease_factor === null ? 2.5 : Number(row.ease_factor),
            repetitions: Number(row.repetitions || 0),
            interval_days: Number(row.interval_days || 0),
            next_review_at: row.next_review_at
        }
    };
}

async function reviewCard(userId, payload) {
    const cardId = Number.parseInt(payload.cardId, 10);
    const quality = Number.parseInt(payload.quality, 10);

    if (!cardId) {
        throw createHttpError(400, 'Thiếu cardId.');
    }

    const [[card]] = await db.query('SELECT id FROM cards WHERE id = ?', [cardId]);
    if (!card) {
        throw createHttpError(404, 'Không tìm thấy flashcard.');
    }

    const [existingRows] = await db.query(`
        SELECT id, ease_factor, repetitions, interval_days
        FROM card_progress
        WHERE user_id = ? AND card_id = ?
        ORDER BY id ASC
        LIMIT 1
    `, [userId, cardId]);
    const existing = existingRows[0];
    const wasNewCard = !existing || Number(existing.repetitions || 0) === 0;
    const learnedIncrement = wasNewCard && quality !== REVIEW_QUALITY.AGAIN ? 1 : 0;
    const reviewedIncrement = !wasNewCard ? 1 : 0;
    const next = calculateSm2Progress(existing, quality);

    const connection = await db.getConnection();
    try {
        await connection.beginTransaction();

        if (existing) {
            await connection.query(`
                UPDATE card_progress
                SET ease_factor = ?, repetitions = ?, interval_days = ?,
                    next_review_at = ?, last_reviewed_at = NOW()
                WHERE id = ?
            `, [next.easeFactor, next.repetitions, next.intervalDays, next.nextReviewAt, existing.id]);
        } else {
            await connection.query(`
                INSERT INTO card_progress
                    (user_id, card_id, ease_factor, repetitions, interval_days, next_review_at, last_reviewed_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
            `, [userId, cardId, next.easeFactor, next.repetitions, next.intervalDays, next.nextReviewAt]);
        }

        await connection.query(`
            INSERT INTO learning_logs (user_id, card_id, quality)
            VALUES (?, ?, ?)
        `, [userId, cardId, quality]);

        await connection.query(`
            INSERT INTO daily_progress
                (user_id, study_date, words_learned, words_reviewed, time_spent_minutes, is_streak_extended)
            VALUES (?, CURDATE(), ?, ?, 0, TRUE)
            ON DUPLICATE KEY UPDATE
                words_learned = words_learned + VALUES(words_learned),
                words_reviewed = words_reviewed + VALUES(words_reviewed),
                is_streak_extended = TRUE
        `, [userId, learnedIncrement, reviewedIncrement]);

        await connection.commit();
    } catch (err) {
        await connection.rollback();
        throw err;
    } finally {
        connection.release();
    }

    await refreshUserStatistics(userId);

    return {
        card_id: cardId,
        quality,
        ease_factor: next.easeFactor,
        repetitions: next.repetitions,
        interval_days: next.intervalDays,
        next_review_at: next.nextReviewAt
    };
}

async function refreshUserStatistics(userId) {
    const [[learned]] = await db.query(`
        SELECT COUNT(*) AS count
        FROM card_progress
        WHERE user_id = ? AND repetitions > 0
    `, [userId]);

    const [[accuracy]] = await db.query(`
        SELECT AVG(CASE WHEN quality >= 2 THEN 1 ELSE 0 END) AS rate
        FROM learning_logs
        WHERE user_id = ?
    `, [userId]);

    const currentStreak = await calculateCurrentStreak(userId);

    await db.query(`
        INSERT INTO user_statistics (user_id, total_words_learned, accuracy_rate, current_streak, max_streak)
        VALUES (?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            total_words_learned = VALUES(total_words_learned),
            accuracy_rate = VALUES(accuracy_rate),
            current_streak = VALUES(current_streak),
            max_streak = GREATEST(max_streak, VALUES(max_streak))
    `, [
        userId,
        learned.count,
        accuracy.rate || 0,
        currentStreak,
        currentStreak
    ]);
}

async function calculateCurrentStreak(userId) {
    const [rows] = await db.query(`
        SELECT DATE_FORMAT(study_date, '%Y-%m-%d') AS study_date
        FROM daily_progress
        WHERE user_id = ? AND (words_learned > 0 OR words_reviewed > 0)
        ORDER BY study_date DESC
        LIMIT 365
    `, [userId]);

    const studiedDates = new Set(rows.map(row => row.study_date));
    const todayStr = toDateKey(new Date());
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = toDateKey(yesterday);

    // If no study today AND no study yesterday, streak is broken (0)
    if (!studiedDates.has(todayStr) && !studiedDates.has(yesterdayStr)) {
        return 0;
    }

    // Start cursor at yesterday if they haven't studied yet today, to keep streak alive
    let cursor = new Date();
    if (!studiedDates.has(todayStr)) {
        cursor = yesterday;
    }

    let streak = 0;
    while (studiedDates.has(toDateKey(cursor))) {
        streak += 1;
        cursor.setDate(cursor.getDate() - 1);
    }

    return streak;
}

function toDateKey(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

module.exports = {
    REVIEW_QUALITY,
    getDailyPlan,
    getDeckSummaries,
    getLearningSession,
    reviewCard,
    refreshUserStatistics
};
