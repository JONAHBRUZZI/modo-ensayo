insert into roles (name) values
('ADMIN'),
('TEACHER'),
('USER')
on conflict do nothing;

insert into users (email, password_hash, full_name)
values
('admin@test.com','hash_admin','Admin Principal'),
('teacher@test.com','hash_teacher','Profesor Demo'),
('user@test.com','hash_user','Usuario Demo')
on conflict (email) do nothing;

insert into user_roles (user_id, role_id)
select u.id, r.id
from users u, roles r
where (u.email='admin@test.com' and r.name='ADMIN')
   or (u.email='teacher@test.com' and r.name='TEACHER')
   or (u.email='user@test.com' and r.name='USER')
on conflict do nothing;

insert into venues (admin_id, name, address, status)
select id, 'Sede Central', 'Santiago Centro', 'APPROVED'
from users where email='admin@test.com'
and not exists (select 1 from venues where name = 'Sede Central');

insert into rooms (venue_id, name, capacity)
select v.id, 'Sala 1', 20
from venues v
where not exists (select 1 from rooms where name = 'Sala 1')
limit 1;

insert into classes (room_id, teacher_id, title, capacity, price, start_time, end_time, status)
select r.id, u.id, 'Clase Yoga', 15, 5000, now(), now() + interval '1 hour', 'PUBLISHED'
from rooms r, users u
where u.email='teacher@test.com'
and not exists (select 1 from classes where title = 'Clase Yoga')
limit 1;

insert into enrollments (class_id, beneficiary_type, beneficiary_id)
select c.id, 'USER', u.id
from classes c, users u
where u.email='user@test.com'
limit 1
on conflict (class_id, beneficiary_type, beneficiary_id) do nothing;

insert into payments (enrollment_id, amount, status)
select e.id, 5000, 'RETAINED'
from enrollments e
where not exists (select 1 from payments p where p.enrollment_id = e.id)
limit 1;
