-- V3: Seed data for local development and demo purposes

-- Locations
INSERT INTO location (id, name, city, country) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'Toronto HQ',       'Toronto',    'Canada'),
    ('a1000000-0000-0000-0000-000000000002', 'Vancouver Office', 'Vancouver',  'Canada'),
    ('a1000000-0000-0000-0000-000000000003', 'London Office',    'London',     'United Kingdom'),
    ('a1000000-0000-0000-0000-000000000004', 'New York Office',  'New York',   'United States'),
    ('a1000000-0000-0000-0000-000000000005', 'Remote',           NULL,         NULL);

-- Users
INSERT INTO user_detail (id, name, email, location_id) VALUES
    ('b1000000-0000-0000-0000-000000000001', 'Alice Sharma',   'alice.sharma@example.com',   'a1000000-0000-0000-0000-000000000001'),
    ('b1000000-0000-0000-0000-000000000002', 'Bob Chen',       'bob.chen@example.com',       'a1000000-0000-0000-0000-000000000001'),
    ('b1000000-0000-0000-0000-000000000003', 'Carol Patel',    'carol.patel@example.com',    'a1000000-0000-0000-0000-000000000002'),
    ('b1000000-0000-0000-0000-000000000004', 'David Osei',     'david.osei@example.com',     'a1000000-0000-0000-0000-000000000003'),
    ('b1000000-0000-0000-0000-000000000005', 'Eva Müller',     'eva.muller@example.com',     'a1000000-0000-0000-0000-000000000004'),
    ('b1000000-0000-0000-0000-000000000006', 'Frank Torres',   'frank.torres@example.com',   'a1000000-0000-0000-0000-000000000005'),
    ('b1000000-0000-0000-0000-000000000007', 'Grace Kim',      'grace.kim@example.com',      'a1000000-0000-0000-0000-000000000002'),
    ('b1000000-0000-0000-0000-000000000008', 'Henry Nakamura', 'henry.nakamura@example.com', NULL);
