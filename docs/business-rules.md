# Business Rules

## Regla 1: Pagos Retenidos (Core)

Todo pago queda en estado `RETAINED` al momento de la compra.
El pago se libera automaticamente (`RELEASED`) solo cuando la clase asociada pasa a estado `COMPLETED`.

**Implementacion**: Trigger `trg_release_payment` en PostgreSQL.

## Regla 2: Control de Capacidad

No se puede inscribir a un beneficiario en una clase que ya alcanzo su capacidad maxima.

**Implementacion**: Trigger `trg_check_capacity` + CHECK al insertar en `enrollments`.

## Regla 3: Auditoria de Estados de Clase

Cada cambio de estado de una clase (`SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`) debe quedar registrado en `class_status_history` con el usuario que realizo el cambio.

**Implementacion**: Trigger `trg_class_status_change`.

## Regla 4: Verificacion de Identidad

Los usuarios que quieran ser TEACHER deben pasar por verificacion de identidad (subir documento, revision por ADMIN).

**Estados**: `PENDING` -> `APPROVED` o `REJECTED`.

## Regla 5: Aprobacion de Sedes

Las sedes ingresadas por TEACHER deben ser aprobadas por un ADMIN antes de ser visibles publicamente.

**Estados**: `PENDING` -> `APPROVED` o `REJECTED`.

## Regla 6: Beneficiarios (Asociados)

Un usuario puede inscribir a sus asociados (familiares/dependientes) en una clase. El pago siempre lo hace el usuario principal.

Los beneficiarios se identifican por tipo (`USER` o `ASSOCIATE`) y su ID correspondiente.

## Regla 7: Roles Dinamicos

Los roles se gestionan dinamicamente. Un ADMIN puede asignar o revocar roles (TEACHER, ADMIN) a cualquier usuario.

## Regla 8: Metodos de Reembolso

Cada usuario debe tener al menos un metodo de reembolso configurado para recibir devoluciones en caso de cancelacion de clase.

## Regla 9: Unicidad de Inscripcion

Un beneficiario no puede inscribirse dos veces en la misma clase.

**Implementacion**: UNIQUE constraint en `enrollments (class_id, beneficiary_type, beneficiary_id)`.

## Flujo de Estados - Pago

```
[Usuario paga] -> RETAINED
                    |
          [clase COMPLETED] -> RELEASED (trigger automatico)
                    |
          [clase CANCELLED] -> REFUND_PENDING -> REFUNDED (manual por admin)
```

## Flujo de Estados - Clase

```
SCHEDULED -> IN_PROGRESS -> COMPLETED
     |                         
     +------> CANCELLED
```
