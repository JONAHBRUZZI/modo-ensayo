create extension if not exists "uuid-ossp";

create table if not exists users (
  id uuid primary key default uuid_generate_v4(),
  email text unique not null,
  password_hash text not null,
  full_name text not null,
  phone text,
  created_at timestamptz default now()
);

create table if not exists roles (
  id serial primary key,
  name text unique not null
);

create table if not exists user_roles (
  user_id uuid references users(id) on delete cascade,
  role_id int references roles(id) on delete cascade,
  primary key (user_id, role_id)
);

create table if not exists identity_verifications (
  id uuid primary key default uuid_generate_v4(),
  user_id uuid references users(id) on delete cascade,
  document_url text not null,
  status text check (status in ('PENDING','APPROVED','REJECTED')),
  reviewed_by uuid references users(id),
  created_at timestamptz default now()
);

create table if not exists associates (
  id uuid primary key default uuid_generate_v4(),
  owner_id uuid references users(id) on delete cascade,
  name text not null,
  relation text,
  birth_date date,
  rut text,
  created_at timestamptz default now()
);

create table if not exists venues (
  id uuid primary key default uuid_generate_v4(),
  admin_id uuid references users(id),
  name text not null,
  address text,
  description text,
  image_url text,
  phone text,
  email text,
  status text check (status in ('PENDING','APPROVED','REJECTED')),
  created_at timestamptz default now()
);

create table if not exists rooms (
  id uuid primary key default uuid_generate_v4(),
  venue_id uuid references venues(id) on delete cascade,
  name text,
  capacity int not null,
  floor_type text,
  has_mirrors boolean default false,
  has_sound boolean default false,
  has_ballet_bar boolean default false,
  has_air_conditioning boolean default false,
  has_natural_light boolean default false,
  lighting text,
  wall_color text,
  image_url text,
  created_at timestamptz default now()
);

create table if not exists room_availability (
  id uuid primary key default uuid_generate_v4(),
  room_id uuid references rooms(id) on delete cascade,
  start_time timestamptz,
  end_time timestamptz
);

create table if not exists classes (
  id uuid primary key default uuid_generate_v4(),
  room_id uuid references rooms(id),
  teacher_id uuid references users(id),
  title text,
  discipline text,
  capacity int,
  price int,
  start_time timestamptz,
  end_time timestamptz,
  status text,
  created_at timestamptz default now()
);

create table if not exists class_status_history (
  id uuid primary key default uuid_generate_v4(),
  class_id uuid references classes(id) on delete cascade,
  previous_status text,
  new_status text,
  changed_by uuid references users(id),
  created_at timestamptz default now()
);

create table if not exists cart_items (
  id uuid primary key default uuid_generate_v4(),
  owner_id uuid references users(id),
  class_id uuid references classes(id),
  beneficiary_type text,
  beneficiary_id uuid,
  created_at timestamptz default now()
);

create table if not exists enrollments (
  id uuid primary key default uuid_generate_v4(),
  class_id uuid references classes(id),
  beneficiary_type text,
  beneficiary_id uuid,
  status text default 'ACTIVE',
  created_at timestamptz default now(),
  unique (class_id, beneficiary_type, beneficiary_id)
);

create table if not exists payments (
  id uuid primary key default uuid_generate_v4(),
  enrollment_id uuid references enrollments(id),
  amount int not null,
  status text,
  created_at timestamptz default now()
);

create table if not exists consolidated_payments (
  id uuid primary key default uuid_generate_v4(),
  user_id uuid references users(id),
  total_amount int,
  status text default 'COMPLETED',
  created_at timestamptz default now()
);

create table if not exists payment_items (
  id uuid primary key default uuid_generate_v4(),
  consolidated_payment_id uuid references consolidated_payments(id),
  payment_id uuid references payments(id)
);

create table if not exists reschedules (
  id uuid primary key default uuid_generate_v4(),
  class_id uuid references classes(id),
  proposed_time timestamptz,
  status text,
  created_at timestamptz default now()
);

create table if not exists reschedule_responses (
  id uuid primary key default uuid_generate_v4(),
  reschedule_id uuid references reschedules(id),
  user_id uuid references users(id),
  response text,
  responded_at timestamptz
);

create table if not exists refund_methods (
  id uuid primary key default uuid_generate_v4(),
  user_id uuid references users(id),
  method text,
  details jsonb
);

create table if not exists notifications (
  id uuid primary key default uuid_generate_v4(),
  user_id uuid references users(id),
  message text,
  read boolean default false,
  created_at timestamptz default now()
);

create table if not exists attendance (
  id uuid primary key default uuid_generate_v4(),
  class_id uuid references classes(id),
  beneficiary_id uuid,
  beneficiary_type text,
  present boolean default true,
  marked_by text,
  created_at timestamptz default now(),
  unique (class_id, beneficiary_id)
);
