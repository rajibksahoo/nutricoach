-- Seeds 10 bodyweight / home-friendly exercises for EVERY coach in the DB,
-- each with a verified YouTube tutorial link (titles checked via oEmbed).
--
-- Exercises are tenant-scoped (coach_id NOT NULL), so "visible to all
-- coaches" is achieved by inserting one copy per coach. Re-runnable: the
-- NOT EXISTS guard skips coaches that already have an exercise by the
-- same name. New coaches need a re-run to receive the library.
--
-- Usage (psql against the local dev DB, from repo root):
--   docker exec -i pg-test psql -U nutricoach -d nutricoach_test < scripts/seed-exercises.sql
-- PowerShell:
--   Get-Content scripts/seed-exercises.sql -Raw | docker exec -i pg-test psql -U nutricoach -d nutricoach_test

BEGIN;

INSERT INTO exercises (
    id, coach_id, name, description, muscle_group, equipment,
    video_url, notes, category, movement_pattern, tags, is_custom,
    created_at, updated_at
)
SELECT
    gen_random_uuid(), c.id, e.name, e.description, e.muscle_group, e.equipment,
    e.video_url, e.notes, e.category::varchar, e.movement_pattern,
    e.tags::jsonb, false, now(), now()
FROM coaches c
CROSS JOIN (VALUES
    (
        'Push-Up',
        'Classic upper-body pressing movement. Hands shoulder-width, body in a straight line from head to heels, lower until chest is just above the floor, press back up.',
        'chest', 'none',
        'https://www.youtube.com/watch?v=IODxDxX7oi4',
        'Scale down with knee push-ups; scale up with feet elevated or archer push-ups. 3 sets of AMRAP.',
        'bodyweight', 'horizontal_push',
        '["beginner","home","upper-body"]'
    ),
    (
        'Bodyweight Squat',
        'Foundational lower-body movement. Feet shoulder-width, toes slightly out, sit back and down keeping chest tall, knees tracking over toes.',
        'quads', 'none',
        'https://www.youtube.com/watch?v=aclHkVaku9U',
        'Aim for thighs parallel or below. Pause 1s at the bottom for added difficulty.',
        'bodyweight', 'squat',
        '["beginner","home","lower-body"]'
    ),
    (
        'Glute Bridge',
        'Lie on back, knees bent, feet flat. Drive through heels to lift hips until body forms a straight line from shoulders to knees. Squeeze glutes hard at the top.',
        'glutes', 'none',
        'https://www.youtube.com/watch?v=wPM8icPu6H8',
        'Posterior-chain activation. Hold 2s at the top. Progress to single-leg version.',
        'bodyweight', 'hinge',
        '["beginner","home","posterior-chain"]'
    ),
    (
        'Reverse Lunge',
        'Standing tall, step one foot back and lower until both knees are at ~90°, front shin vertical. Push through the front heel to return.',
        'quads', 'none',
        'https://www.youtube.com/watch?v=QOVaHwm-Q6U',
        'Easier on knees than forward lunges. 8-10 reps per leg.',
        'bodyweight', 'lunge',
        '["beginner","home","unilateral"]'
    ),
    (
        'Plank',
        'Forearms on the ground, elbows under shoulders, body in a straight line. Brace the core, squeeze glutes, breathe normally. Avoid sagging hips or piking up.',
        'core', 'none',
        'https://www.youtube.com/watch?v=ASdvN_XEl_c',
        'Start with 20-30s holds, build to 60s. Quality over duration — break the set if form breaks.',
        'timed', 'core_anti_extension',
        '["beginner","home","core","isometric"]'
    ),
    (
        'Mountain Climbers',
        'From a high plank, drive knees toward chest alternately at a quick pace. Keep hips low and core tight; avoid bouncing the butt up.',
        'core', 'none',
        'https://www.youtube.com/watch?v=nmwgirgXLYM',
        'Conditioning + core. 30s on, 30s rest. Slow tempo for core focus, fast for cardio.',
        'cardio', 'core_dynamic',
        '["intermediate","home","cardio","conditioning"]'
    ),
    (
        'Bird Dog',
        'On hands and knees, extend opposite arm and leg until parallel to floor, hold briefly, return. Spine stays neutral throughout — no twisting at the hips.',
        'core', 'none',
        'https://www.youtube.com/watch?v=wiFNA3sqjCA',
        'Spinal stability + glute activation. 8-10 reps per side, controlled tempo.',
        'bodyweight', 'core_anti_rotation',
        '["beginner","home","core","mobility"]'
    ),
    (
        'Superman',
        'Lie face-down, arms extended overhead. Lift arms, chest, and legs off the floor simultaneously, squeeze the lower back and glutes, hold 1-2s, lower with control.',
        'back', 'none',
        'https://www.youtube.com/watch?v=z6PJMT2y8GQ',
        'Posterior-chain endurance. 12-15 reps. Pair with planks for balanced core work.',
        'bodyweight', 'core_extension',
        '["beginner","home","back","posterior-chain"]'
    ),
    (
        'Dead Bug',
        'On back, arms straight up, knees bent 90°. Slowly lower opposite arm and leg toward floor while pressing lower back into the ground. Return and switch sides.',
        'core', 'none',
        'https://www.youtube.com/watch?v=4XLEnwUr1d8',
        'Anti-extension core control. 6-8 reps per side, very slow tempo.',
        'bodyweight', 'core_anti_extension',
        '["beginner","home","core","stability"]'
    ),
    (
        'Burpees',
        'From standing, squat down, kick feet back to a plank, perform a push-up (optional), jump feet forward, then explode up into a jump with hands overhead.',
        'full_body', 'none',
        'https://www.youtube.com/watch?v=dZgVxmf6jkA',
        'Full-body conditioning. 5-10 reps per round. Step back instead of jumping back to scale down.',
        'cardio', 'full_body_conditioning',
        '["intermediate","home","cardio","conditioning"]'
    )
) AS e(name, description, muscle_group, equipment, video_url, notes, category, movement_pattern, tags)
WHERE c.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM exercises x
    WHERE x.coach_id = c.id AND x.name = e.name AND x.deleted_at IS NULL
);

-- Verify: per-coach exercise counts
SELECT c.name AS coach, count(x.id) AS exercises
FROM coaches c
LEFT JOIN exercises x ON x.coach_id = c.id AND x.deleted_at IS NULL
WHERE c.deleted_at IS NULL
GROUP BY c.name
ORDER BY c.name;

COMMIT;
