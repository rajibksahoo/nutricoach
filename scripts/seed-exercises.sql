-- Seeds 10 bodyweight / home-friendly exercises for a single coach.
--
-- Usage (psql against the local dev DB):
--   docker exec -i pg-test psql -U nutricoach -d nutricoach_test \
--     -v coach_phone="'9999999999'" -f seed-exercises.sql
--
-- Or inside psql:
--   \set coach_phone '''9999999999'''
--   \i seed-exercises.sql
--
-- video_url is a YouTube SEARCH URL, not a specific video. Each search is
-- crafted to surface the canonical tutorial from a reputable channel
-- (Athlean-X, Calisthenicmovement, Squat University, etc.) as the top
-- result. Click, vet, and replace with the specific watch?v=... URL.

BEGIN;

WITH target_coach AS (
    SELECT id FROM coaches WHERE phone = :coach_phone AND deleted_at IS NULL
)
INSERT INTO exercises (
    id, coach_id, name, description, muscle_group, equipment,
    video_url, notes, category, movement_pattern, tags, is_custom,
    created_at, updated_at
)
SELECT
    gen_random_uuid(), tc.id, e.name, e.description, e.muscle_group, e.equipment,
    e.video_url, e.notes, e.category::varchar, e.movement_pattern,
    e.tags::jsonb, true, now(), now()
FROM target_coach tc
CROSS JOIN (VALUES
    (
        'Push-Up',
        'Classic upper-body pressing movement. Hands shoulder-width, body in a straight line from head to heels, lower until chest is just above the floor, press back up.',
        'chest', 'none',
        'https://www.youtube.com/results?search_query=calisthenicmovement+perfect+push+up+tutorial',
        'Scale down with knee push-ups; scale up with feet elevated or archer push-ups. 3 sets of AMRAP.',
        'bodyweight', 'horizontal_push',
        '["beginner","home","upper-body"]'
    ),
    (
        'Bodyweight Squat',
        'Foundational lower-body movement. Feet shoulder-width, toes slightly out, sit back and down keeping chest tall, knees tracking over toes.',
        'quads', 'none',
        'https://www.youtube.com/results?search_query=squat+university+bodyweight+squat+form',
        'Aim for thighs parallel or below. Pause 1s at the bottom for added difficulty.',
        'bodyweight', 'squat',
        '["beginner","home","lower-body"]'
    ),
    (
        'Glute Bridge',
        'Lie on back, knees bent, feet flat. Drive through heels to lift hips until body forms a straight line from shoulders to knees. Squeeze glutes hard at the top.',
        'glutes', 'none',
        'https://www.youtube.com/results?search_query=athlean+x+glute+bridge+how+to',
        'Posterior-chain activation. Hold 2s at the top. Progress to single-leg version.',
        'bodyweight', 'hinge',
        '["beginner","home","posterior-chain"]'
    ),
    (
        'Reverse Lunge',
        'Standing tall, step one foot back and lower until both knees are at ~90°, front shin vertical. Push through the front heel to return.',
        'quads', 'none',
        'https://www.youtube.com/results?search_query=reverse+lunge+proper+form+tutorial',
        'Easier on knees than forward lunges. 8-10 reps per leg.',
        'bodyweight', 'lunge',
        '["beginner","home","unilateral"]'
    ),
    (
        'Plank',
        'Forearms on the ground, elbows under shoulders, body in a straight line. Brace the core, squeeze glutes, breathe normally. Avoid sagging hips or piking up.',
        'core', 'none',
        'https://www.youtube.com/results?search_query=calisthenicmovement+plank+tutorial+form',
        'Start with 20-30s holds, build to 60s. Quality over duration — break the set if form breaks.',
        'timed', 'core_anti_extension',
        '["beginner","home","core","isometric"]'
    ),
    (
        'Mountain Climbers',
        'From a high plank, drive knees toward chest alternately at a quick pace. Keep hips low and core tight; avoid bouncing the butt up.',
        'core', 'none',
        'https://www.youtube.com/results?search_query=mountain+climbers+proper+form+tutorial',
        'Conditioning + core. 30s on, 30s rest. Slow tempo for core focus, fast for cardio.',
        'cardio', 'core_dynamic',
        '["intermediate","home","cardio","conditioning"]'
    ),
    (
        'Bird Dog',
        'On hands and knees, extend opposite arm and leg until parallel to floor, hold briefly, return. Spine stays neutral throughout — no twisting at the hips.',
        'core', 'none',
        'https://www.youtube.com/results?search_query=bird+dog+exercise+tutorial+mcgill',
        'Spinal stability + glute activation. 8-10 reps per side, controlled tempo.',
        'bodyweight', 'core_anti_rotation',
        '["beginner","home","core","mobility"]'
    ),
    (
        'Superman',
        'Lie face-down, arms extended overhead. Lift arms, chest, and legs off the floor simultaneously, squeeze the lower back and glutes, hold 1-2s, lower with control.',
        'back', 'none',
        'https://www.youtube.com/results?search_query=superman+exercise+lower+back+tutorial',
        'Posterior-chain endurance. 12-15 reps. Pair with planks for balanced core work.',
        'bodyweight', 'core_extension',
        '["beginner","home","back","posterior-chain"]'
    ),
    (
        'Dead Bug',
        'On back, arms straight up, knees bent 90°. Slowly lower opposite arm and leg toward floor while pressing lower back into the ground. Return and switch sides.',
        'core', 'none',
        'https://www.youtube.com/results?search_query=dead+bug+exercise+tutorial+core+stability',
        'Anti-extension core control. 6-8 reps per side, very slow tempo.',
        'bodyweight', 'core_anti_extension',
        '["beginner","home","core","stability"]'
    ),
    (
        'Burpees',
        'From standing, squat down, kick feet back to a plank, perform a push-up (optional), jump feet forward, then explode up into a jump with hands overhead.',
        'full_body', 'none',
        'https://www.youtube.com/results?search_query=burpee+proper+form+tutorial+athlean+x',
        'Full-body conditioning. 5-10 reps per round. Step back instead of jumping back to scale down.',
        'cardio', 'full_body_conditioning',
        '["intermediate","home","cardio","conditioning"]'
    )
) AS e(name, description, muscle_group, equipment, video_url, notes, category, movement_pattern, tags)
WHERE NOT EXISTS (
    SELECT 1 FROM exercises x
    WHERE x.coach_id = tc.id AND x.name = e.name AND x.deleted_at IS NULL
);

-- Verify
SELECT name, category, muscle_group, video_url
FROM exercises
WHERE coach_id = (SELECT id FROM coaches WHERE phone = :coach_phone)
  AND deleted_at IS NULL
ORDER BY created_at DESC
LIMIT 10;

COMMIT;
