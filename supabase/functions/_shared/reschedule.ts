// deno-lint-ignore-file no-explicit-any
// Helper compartido: dispara la decisión del alumno tras completarse un
// reagendamiento (la clase ya se movió al nuevo horario). Crea una fila
// `reschedules` (TEACHER_ACCEPTED + 48h) y una `reschedule_responses` pendiente
// por cada alumno inscrito, y los notifica. Reengancha con el flujo existente
// `student-decision` + el cron `process_reschedule_timeouts` (aceptar/rechazar
// con reembolso al que rechaza o no responde en 48h).
export async function triggerStudentReschedule(
  admin: any,
  opts: { classId: string; teacherId: string; newStartTime: string; reason: string },
): Promise<void> {
  const deadline = new Date(Date.now() + 48 * 3600 * 1000).toISOString();

  const { data: resched } = await admin.from("reschedules").insert({
    class_id: opts.classId,
    teacher_id: opts.teacherId,
    proposed_time: opts.newStartTime,
    reason: opts.reason,
    status: "TEACHER_ACCEPTED",
    response_deadline: deadline,
  }).select("id").single();
  if (!resched) return;

  const { data: enrolled } = await admin.from("enrollments")
    .select("student_id").eq("class_id", opts.classId).eq("status", "ACTIVE");

  for (const e of enrolled ?? []) {
    await admin.from("reschedule_responses").insert({
      reschedule_id: resched.id,
      user_id: e.student_id,
      response_type: null,
    });
    await admin.from("notifications").insert({
      user_id: e.student_id,
      title: "Confirma el reagendamiento",
      message: "Tu clase fue reagendada a un nuevo horario. Tienes 48h para aceptar o rechazar (con reembolso).",
      type: "RESCHEDULE_PENDING",
    });
  }
}
