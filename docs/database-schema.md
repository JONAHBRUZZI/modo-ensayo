# Database Schema

## Tablas

### users
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK, default uuid_generate_v4() |
| email | TEXT | UNIQUE, NOT NULL |
| password_hash | TEXT | NOT NULL |
| full_name | TEXT | NOT NULL |
| phone | TEXT | |
| created_at | TIMESTAMPTZ | DEFAULT now() |

### roles
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | SERIAL | PK |
| name | TEXT | UNIQUE, NOT NULL |

### user_roles
| Columna | Tipo | Constraints |
|---------|------|------------|
| user_id | UUID | PK, FK -> users(id) ON DELETE CASCADE |
| role_id | INT | PK, FK -> roles(id) ON DELETE CASCADE |

### identity_verifications
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| user_id | UUID | FK -> users(id) ON DELETE CASCADE |
| document_url | TEXT | NOT NULL |
| status | TEXT | CHECK IN ('PENDING','APPROVED','REJECTED') |
| reviewed_by | UUID | FK -> users(id) |
| created_at | TIMESTAMPTZ | DEFAULT now() |

### associates
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| owner_id | UUID | FK -> users(id) ON DELETE CASCADE |
| name | TEXT | NOT NULL |
| relation | TEXT | |
| birth_date | DATE | |
| rut | TEXT | |
| created_at | TIMESTAMPTZ | DEFAULT now() |

### venues
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| admin_id | UUID | FK -> users(id) |
| name | TEXT | NOT NULL |
| address | TEXT | |
| description | TEXT | |
| image_url | TEXT | |
| phone | TEXT | |
| email | TEXT | |
| status | TEXT | CHECK IN ('PENDING','APPROVED','REJECTED') |
| created_at | TIMESTAMPTZ | DEFAULT now() |

### rooms
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| venue_id | UUID | FK -> venues(id) ON DELETE CASCADE |
| name | TEXT | |
| capacity | INT | NOT NULL |
| floor_type | TEXT | |
| has_mirrors | BOOLEAN | DEFAULT false |
| has_sound | BOOLEAN | DEFAULT false |
| has_ballet_bar | BOOLEAN | DEFAULT false |
| has_air_conditioning | BOOLEAN | DEFAULT false |
| has_natural_light | BOOLEAN | DEFAULT false |
| lighting | TEXT | |
| wall_color | TEXT | |
| image_url | TEXT | |
| created_at | TIMESTAMPTZ | DEFAULT now() |

### classes
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| room_id | UUID | FK -> rooms(id) |
| teacher_id | UUID | FK -> users(id) |
| title | TEXT | |
| discipline | TEXT | |
| capacity | INT | |
| price | INT | |
| start_time | TIMESTAMPTZ | |
| end_time | TIMESTAMPTZ | |
| status | TEXT | |
| created_at | TIMESTAMPTZ | DEFAULT now() |

### class_status_history
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| class_id | UUID | FK -> classes(id) ON DELETE CASCADE |
| previous_status | TEXT | |
| new_status | TEXT | |
| changed_by | UUID | FK -> users(id) |
| created_at | TIMESTAMPTZ | DEFAULT now() |

### cart_items
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| owner_id | UUID | FK -> users(id) |
| class_id | UUID | FK -> classes(id) |
| beneficiary_type | TEXT | |
| beneficiary_id | UUID | |
| created_at | TIMESTAMPTZ | DEFAULT now() |

### enrollments
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| class_id | UUID | FK -> classes(id) |
| beneficiary_type | TEXT | |
| beneficiary_id | UUID | |
| status | TEXT | DEFAULT 'ACTIVE' |
| created_at | TIMESTAMPTZ | DEFAULT now() |
| | | UNIQUE (class_id, beneficiary_type, beneficiary_id) |

### payments
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| enrollment_id | UUID | FK -> enrollments(id) |
| amount | INT | NOT NULL |
| status | TEXT | |
| created_at | TIMESTAMPTZ | DEFAULT now() |

### consolidated_payments
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| user_id | UUID | FK -> users(id) |
| total_amount | INT | |
| status | TEXT | DEFAULT 'COMPLETED' |
| created_at | TIMESTAMPTZ | DEFAULT now() |

### payment_items
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| consolidated_payment_id | UUID | FK -> consolidated_payments(id) |
| payment_id | UUID | FK -> payments(id) |

### refund_methods
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| user_id | UUID | FK -> users(id) |
| method | TEXT | |
| details | JSONB | |

### notifications
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| user_id | UUID | FK -> users(id) |
| message | TEXT | |
| read | BOOLEAN | DEFAULT false |
| created_at | TIMESTAMPTZ | DEFAULT now() |

### reschedules
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| class_id | UUID | FK -> classes(id) |
| proposed_time | TIMESTAMPTZ | |
| status | TEXT | |
| created_at | TIMESTAMPTZ | DEFAULT now() |

### reschedule_responses
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| reschedule_id | UUID | FK -> reschedules(id) |
| user_id | UUID | FK -> users(id) |
| response | TEXT | |
| responded_at | TIMESTAMPTZ | |

### room_availability
| Columna | Tipo | Constraints |
|---------|------|------------|
| id | UUID | PK |
| room_id | UUID | FK -> rooms(id) ON DELETE CASCADE |
| start_time | TIMESTAMPTZ | |
| end_time | TIMESTAMPTZ | |

## Triggers y Procedimientos

### trg_class_status_change
Registra automaticamente cada cambio de estado de una clase en `class_status_history`.

### trg_release_payment
Cuando una clase pasa a `COMPLETED`, libera automaticamente todos los pagos asociados de `RETAINED` a `RELEASED`.

### trg_check_capacity
Antes de insertar una inscripcion, verifica que la clase no este llena. Lanza error `'La clase esta llena'` si se excede la capacidad.

### get_user_roles(uuid)
Funcion utilitaria que retorna los roles de un usuario dado su ID.
