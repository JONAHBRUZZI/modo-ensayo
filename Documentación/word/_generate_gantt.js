/**
 * Genera Carta Gantt Actualizada - Modo Ensayo
 * Estado: 30-may-2026 (fin Sprint 5)
 */
const fs = require('fs');
const path = require('path');
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, PageOrientation, LevelFormat,
  HeadingLevel, BorderStyle, WidthType, ShadingType, PageBreak, PageNumber
} = require('docx');

// Helpers
const PAGE = { size: { width: 12240, height: 15840 }, margin: { top: 1080, right: 1080, bottom: 1080, left: 1080 } };
const CONTENT_W = 12240 - 2160; // 10080 DXA

const border = (color = "BFBFBF") => ({ style: BorderStyle.SINGLE, size: 4, color });
const allBorders = (color) => ({ top: border(color), bottom: border(color), left: border(color), right: border(color) });

const cellMargins = { top: 80, bottom: 80, left: 120, right: 120 };

function p(text, opts = {}) {
  const runs = Array.isArray(text)
    ? text
    : [new TextRun({ text, bold: opts.bold, color: opts.color, size: opts.size, italics: opts.italics })];
  return new Paragraph({
    children: runs,
    alignment: opts.align,
    spacing: opts.spacing,
    heading: opts.heading,
    numbering: opts.numbering
  });
}

function tc(text, opts = {}) {
  const widthVal = opts.width || CONTENT_W;
  return new TableCell({
    width: { size: widthVal, type: WidthType.DXA },
    borders: allBorders(opts.borderColor || "BFBFBF"),
    shading: opts.fill ? { fill: opts.fill, type: ShadingType.CLEAR, color: "auto" } : undefined,
    margins: cellMargins,
    verticalAlign: "center",
    children: (Array.isArray(text) ? text : [text]).map(t =>
      typeof t === 'string'
        ? p(t, { bold: opts.bold, size: opts.size, color: opts.color, align: opts.align })
        : t
    )
  });
}

function bullet(text) {
  return new Paragraph({
    numbering: { reference: "bullets", level: 0 },
    children: [new TextRun({ text })]
  });
}

function h1(text) { return p(text, { heading: HeadingLevel.HEADING_1 }); }
function h2(text) { return p(text, { heading: HeadingLevel.HEADING_2 }); }
function h3(text) { return p(text, { heading: HeadingLevel.HEADING_3 }); }

// Color codes
const COLOR_DONE = "C6EFCE";    // light green
const COLOR_PROGRESS = "FFEB9C"; // light yellow
const COLOR_PENDING = "F2F2F2";  // light gray
const COLOR_EXTRA = "DCE6F1";    // light blue
const COLOR_HEADER = "2E75B6";   // dark blue
const COLOR_HEADER_TEXT = "FFFFFF";

// === PORTADA ===
const portada = [
  p([new TextRun({ text: "CARTA GANTT ACTUALIZADA", bold: true, size: 48, color: "2E75B6" })], { align: AlignmentType.CENTER }),
  p([new TextRun({ text: "Proyecto Modo Ensayo", bold: true, size: 36 })], { align: AlignmentType.CENTER }),
  p([new TextRun({ text: "Sprint 0 + 11 Sprints de Desarrollo", size: 24, italics: true })], { align: AlignmentType.CENTER }),
  p(" "),
  p([new TextRun({ text: "Estado al sábado 30 de mayo de 2026", bold: true, size: 28, color: "C00000" })], { align: AlignmentType.CENTER }),
  p([new TextRun({ text: "(Fin de Sprint 5 - Semana 12)", size: 22 })], { align: AlignmentType.CENTER }),
  p(" "),
];

// === Tabla info del proyecto ===
const infoTable = new Table({
  width: { size: CONTENT_W, type: WidthType.DXA },
  columnWidths: [3360, 6720],
  rows: [
    new TableRow({ children: [tc("Proyecto", { width: 3360, bold: true, fill: "F2F2F2" }), tc("Modo Ensayo - Plataforma de gestión de clases de danza y música", { width: 6720 })] }),
    new TableRow({ children: [tc("Equipo", { width: 3360, bold: true, fill: "F2F2F2" }), tc("Darlette Morales · Jonathan Guerra · Victor Silva", { width: 6720 })] }),
    new TableRow({ children: [tc("Asignatura", { width: 3360, bold: true, fill: "F2F2F2" }), tc("Taller Aplicado de Programación - TPY1101", { width: 6720 })] }),
    new TableRow({ children: [tc("Profesor", { width: 3360, bold: true, fill: "F2F2F2" }), tc("Felipe Arturo Castillo Ducaud", { width: 6720 })] }),
    new TableRow({ children: [tc("Sprint 0", { width: 3360, bold: true, fill: "F2F2F2" }), tc("✓ Mié 22-abr a Sáb 25-abr 2026 (3 días) - COMPLETADO", { width: 6720, color: "008000" })] }),
    new TableRow({ children: [tc("Desarrollo", { width: 3360, bold: true, fill: "F2F2F2" }), tc("Semana 8 (27-abr) a Semana 18 (12-jul) - 11 sprints semanales", { width: 6720 })] }),
    new TableRow({ children: [tc("Sprint Actual", { width: 3360, bold: true, fill: "F2F2F2" }), tc("Sprint 5 (Semana 12) - 25-may a 31-may 2026", { width: 6720, color: "0070C0", bold: true })] }),
    new TableRow({ children: [tc("Metodología", { width: 3360, bold: true, fill: "F2F2F2" }), tc("Scrum con sprints semanales (Planning lun, Check mié, Demo sáb, Retro dom)", { width: 6720 })] }),
  ]
});

// === ESTADO GENERAL DE PROGRESO ===
const estadoGeneral = [
  new Paragraph({ children: [new PageBreak()] }),
  h1("1. Estado General del Proyecto al 30-may-2026"),
  p("Al cierre del Sprint 5 (sábado 30 de mayo) el proyecto presenta un estado de avance significativamente por encima del plan original. No solo se han cumplido los Definition of Done de los Sprints 0 a 4, sino que durante los Sprints 3, 4 y 5 se implementaron de forma anticipada múltiples funcionalidades originalmente previstas para los Sprints 6, 7 y 8 (reagendamiento con timeout de 48h, sistema de reseñas, dashboard administrativo, validación de identidad reforzada, entre otros)."),
  p("Adicionalmente, se incorporaron al producto 25+ funcionalidades fuera del plan original que surgieron del análisis exhaustivo de Reglas de Negocio y Casos de Uso durante el desarrollo. Estas se detallan en la Sección 7 de este documento."),
  p(" ")
];

// Tabla resumen ejecutivo de progreso
const resumenProgresoTable = new Table({
  width: { size: CONTENT_W, type: WidthType.DXA },
  columnWidths: [1080, 1620, 2160, 1620, 3600],
  rows: [
    new TableRow({
      tableHeader: true,
      children: [
        tc("Sprint", { width: 1080, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
        tc("Semana", { width: 1620, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
        tc("Foco principal", { width: 2160, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
        tc("Estado", { width: 1620, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
        tc("Notas", { width: 3600, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
      ]
    }),
    new TableRow({ children: [
      tc("S0", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE }),
      tc("22-25 abr", { width: 1620, align: AlignmentType.CENTER, fill: COLOR_DONE }),
      tc("Documentación Diseño", { width: 2160, fill: COLOR_DONE }),
      tc("✓ COMPLETADO", { width: 1620, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE, color: "008000" }),
      tc("4 entregables UML + Requisitos entregados el 25-abr", { width: 3600, fill: COLOR_DONE })
    ]}),
    new TableRow({ children: [
      tc("S1", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE }),
      tc("27-abr/3-may", { width: 1620, align: AlignmentType.CENTER, fill: COLOR_DONE }),
      tc("Setup + Auth + JWT", { width: 2160, fill: COLOR_DONE }),
      tc("✓ COMPLETADO", { width: 1620, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE, color: "008000" }),
      tc("Backend Spring Boot + Frontend Vue + PostgreSQL operativos en cloud", { width: 3600, fill: COLOR_DONE })
    ]}),
    new TableRow({ children: [
      tc("S2", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE }),
      tc("4-10 may", { width: 1620, align: AlignmentType.CENTER, fill: COLOR_DONE }),
      tc("Usuarios + Sedes + Docs", { width: 2160, fill: COLOR_DONE }),
      tc("✓ COMPLETADO", { width: 1620, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE, color: "008000" }),
      tc("Validación identidad + Asociados + CRUD Sede/Sala + Upload seguro", { width: 3600, fill: COLOR_DONE })
    ]}),
    new TableRow({ children: [
      tc("S3", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE }),
      tc("11-17 may", { width: 1620, align: AlignmentType.CENTER, fill: COLOR_DONE }),
      tc("Clases + Búsqueda + Carrito", { width: 2160, fill: COLOR_DONE }),
      tc("✓ COMPLETADO", { width: 1620, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE, color: "008000" }),
      tc("Creación clases por Maestro y Sede, filtros, carrito familiar", { width: 3600, fill: COLOR_DONE })
    ]}),
    new TableRow({ children: [
      tc("S4", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE }),
      tc("18-24 may", { width: 1620, align: AlignmentType.CENTER, fill: COLOR_DONE }),
      tc("Pago consolidado + EXP. 2", { width: 2160, fill: COLOR_DONE }),
      tc("✓ COMPLETADO", { width: 1620, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE, color: "008000" }),
      tc("MercadoPago real (no simulado) + Checkout + Experiencia 2 presentada", { width: 3600, fill: COLOR_DONE })
    ]}),
    new TableRow({ children: [
      tc("S5", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_PROGRESS }),
      tc("25-31 may", { width: 1620, align: AlignmentType.CENTER, fill: COLOR_PROGRESS }),
      tc("Validación Clases + Liberación pagos", { width: 2160, fill: COLOR_PROGRESS }),
      tc("EN PROGRESO (95%)", { width: 1620, bold: true, align: AlignmentType.CENTER, fill: COLOR_PROGRESS, color: "B85C00" }),
      tc("Demo sábado 30-may. Estados clase + liberación + dashboard sede completados", { width: 3600, fill: COLOR_PROGRESS })
    ]}),
    new TableRow({ children: [
      tc("S6", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_EXTRA }),
      tc("1-7 jun", { width: 1620, align: AlignmentType.CENTER, fill: COLOR_EXTRA }),
      tc("Reagendamiento + Timeout 48h", { width: 2160, fill: COLOR_EXTRA }),
      tc("► ADELANTADO 80%", { width: 1620, bold: true, align: AlignmentType.CENTER, fill: COLOR_EXTRA, color: "0070C0" }),
      tc("R19 + scheduled tasks + confirmación R14 ya implementados", { width: 3600, fill: COLOR_EXTRA })
    ]}),
    new TableRow({ children: [
      tc("S7", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_EXTRA }),
      tc("8-14 jun", { width: 1620, align: AlignmentType.CENTER, fill: COLOR_EXTRA }),
      tc("Admin General + Notificaciones", { width: 2160, fill: COLOR_EXTRA }),
      tc("► ADELANTADO 60%", { width: 1620, bold: true, align: AlignmentType.CENTER, fill: COLOR_EXTRA, color: "0070C0" }),
      tc("Panel Admin + notificaciones in-app parciales ya en producción", { width: 3600, fill: COLOR_EXTRA })
    ]}),
    new TableRow({ children: [
      tc("S8", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_EXTRA }),
      tc("15-21 jun", { width: 1620, align: AlignmentType.CENTER, fill: COLOR_EXTRA }),
      tc("Reputación + Mejoras UX", { width: 2160, fill: COLOR_EXTRA }),
      tc("► ADELANTADO 50%", { width: 1620, bold: true, align: AlignmentType.CENTER, fill: COLOR_EXTRA, color: "0070C0" }),
      tc("Reviews backend + frontend implementados. UX en mejora continua", { width: 3600, fill: COLOR_EXTRA })
    ]}),
    new TableRow({ children: [
      tc("S9", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_PENDING }),
      tc("22-28 jun", { width: 1620, align: AlignmentType.CENTER, fill: COLOR_PENDING }),
      tc("QA integral", { width: 2160, fill: COLOR_PENDING }),
      tc("○ PENDIENTE", { width: 1620, bold: true, align: AlignmentType.CENTER, fill: COLOR_PENDING }),
      tc("Tests unitarios backend (Payment, ClassConfirmation, Reschedule) ya creados", { width: 3600, fill: COLOR_PENDING })
    ]}),
    new TableRow({ children: [
      tc("S10", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_PENDING }),
      tc("29-jun/5-jul", { width: 1620, align: AlignmentType.CENTER, fill: COLOR_PENDING }),
      tc("Pulido + Defensa", { width: 2160, fill: COLOR_PENDING }),
      tc("○ PENDIENTE", { width: 1620, bold: true, align: AlignmentType.CENTER, fill: COLOR_PENDING }),
      tc("Video demo + PPT + ensayos", { width: 3600, fill: COLOR_PENDING })
    ]}),
    new TableRow({ children: [
      tc("S11", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_PENDING }),
      tc("6-12 jul", { width: 1620, align: AlignmentType.CENTER, fill: COLOR_PENDING }),
      tc("EVALUACIÓN FINAL (40%)", { width: 2160, fill: COLOR_PENDING, bold: true }),
      tc("○ PENDIENTE", { width: 1620, bold: true, align: AlignmentType.CENTER, fill: COLOR_PENDING }),
      tc("Defensa MVP completo ante docente", { width: 3600, fill: COLOR_PENDING })
    ]}),
  ]
});

// === RESUMEN EJECUTIVO ACTUALIZADO ===
const resumenEjecutivo = [
  new Paragraph({ children: [new PageBreak()] }),
  h1("2. Resumen Ejecutivo Actualizado"),
  p("Esta Carta Gantt organiza el desarrollo del proyecto Modo Ensayo en dos grandes fases. La primera fase (Sprint 0, 22-25 abril) consistió en producir la documentación de diseño completa exigida por el docente como prerrequisito al desarrollo. La segunda fase consta de 11 sprints semanales de desarrollo siguiendo metodología Scrum, hasta la defensa final."),
  p("La planificación consideró el MVP formal acordado: ciclo de vida completo de la clase, pagos condicionados a validación, reagendamiento con consentimiento del alumno, roles dinámicos, especialización en danza y música, carrito familiar con pagos consolidados distribuidos, confirmaciones explícitas en cada decisión irreversible, timeout de 48 horas para reagendamientos y método de devolución preferido por el alumno."),
  p([new TextRun({ text: "Estado actual al 30-may-2026: ", bold: true }), new TextRun({ text: "el proyecto avanza con holgura. Los Sprints 0 a 4 fueron entregados sin retrasos. La Experiencia 2 (35%) fue defendida exitosamente la semana del 18-24 may. El Sprint 5 cierra hoy con demo. Las funcionalidades del Sprint 6 (Reagendamiento), Sprint 7 (Admin General) y Sprint 8 (Reputación) ya están parcial o totalmente operativas, lo que libera capacidad para ampliar QA, documentación y pulido final." })]),
  p(" ")
];

// === EQUIPO ===
const equipoSection = [
  h1("3. Equipo y Responsabilidades"),
  new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: [2160, 1620, 6300],
    rows: [
      new TableRow({ tableHeader: true, children: [
        tc("Integrante", { width: 2160, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
        tc("Área", { width: 1620, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
        tc("Responsabilidades", { width: 6300, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER })
      ]}),
      new TableRow({ children: [
        tc("Victor Silva", { width: 2160, bold: true }),
        tc("Frontend", { width: 1620 }),
        tc("Sprint 0: Diagramas Casos de Uso, Diagramas de Actividad, Mockups iniciales. Desarrollo: Vue 3 + Vite + Tailwind, componentes, UX, integración API REST", { width: 6300 })
      ]}),
      new TableRow({ children: [
        tc("Jonathan Guerra", { width: 2160, bold: true }),
        tc("Backend", { width: 1620 }),
        tc("Sprint 0: Diagrama de Clases, Diagramas de Secuencia, Requisitos Funcionales. Desarrollo: Spring Boot 3.2 + Java 21, API REST, lógica de negocio, MercadoPago", { width: 6300 })
      ]}),
      new TableRow({ children: [
        tc("Darlette Morales", { width: 2160, bold: true }),
        tc("BD + DevOps", { width: 1620 }),
        tc("Sprint 0: Diagrama de Arquitectura, Diagrama de Componentes, Requisitos No Funcionales. Desarrollo: PostgreSQL 16, P.A., Docker, despliegue cloud", { width: 6300 })
      ]})
    ]
  }),
  p(" "),
  p([new TextRun({ text: "Nota: ", bold: true }), new TextRun("Las Reglas de Negocio e Historias de Usuario del Sprint 0 se redactaron colaborativamente entre los tres integrantes.")]),
  p(" ")
];

// === PANORAMA GANTT ===
function ganttCell(estado) {
  const colors = {
    'D': COLOR_DONE,        // Done
    'P': COLOR_PROGRESS,    // In progress
    '_': COLOR_PENDING,     // Pending
    'A': COLOR_EXTRA,       // Ahead of schedule
    'X': "FFFFFF"           // empty
  };
  const symbols = { 'D': '✓', 'P': '●', '_': '', 'A': '►', 'X': '' };
  return tc(symbols[estado], { width: 540, fill: colors[estado], align: AlignmentType.CENTER, bold: true });
}

function moduloRow(modulo, estados, sprintColor) {
  return new TableRow({ children: [
    tc(modulo, { width: 3060, bold: true, size: 18 }),
    ...estados.map(e => ganttCell(e))
  ]});
}

const panoramaTable = new Table({
  width: { size: CONTENT_W, type: WidthType.DXA },
  columnWidths: [3060, 540, 540, 540, 540, 540, 540, 540, 540, 540, 540, 540, 540, 540],
  rows: [
    new TableRow({ tableHeader: true, children: [
      tc("Módulo / Período", { width: 3060, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER, size: 18 }),
      tc("S0", { width: 540, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
      ...["8","9","10","11","12","13","14","15","16","17","18"].map(n =>
        tc(n, { width: 540, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER })
      ),
      tc("Est.", { width: 540, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER, size: 16 })
    ]}),
    moduloRow("DOCUMENTACIÓN DE DISEÑO",         ['D','X','X','X','X','X','X','X','X','X','X','X']),
    moduloRow("Setup y Arquitectura",            ['X','D','X','X','X','X','X','X','X','X','X','X']),
    moduloRow("Autenticación y Roles",           ['X','D','D','X','X','X','X','X','X','X','X','X']),
    moduloRow("Asociados + Documentos seguros",  ['X','X','D','X','X','X','X','X','X','X','X','X']),
    moduloRow("Sedes y Salas (precondición)",    ['X','X','D','D','X','X','X','X','X','X','X','X']),
    moduloRow("Clases (crear/publicar)",         ['X','X','X','D','X','X','X','X','X','X','X','X']),
    moduloRow("Búsqueda y filtros",              ['X','X','X','D','X','X','X','X','X','X','X','X']),
    moduloRow("Carrito de compra",               ['X','X','X','D','D','X','X','X','X','X','X','X']),
    moduloRow("Pago consolidado (MercadoPago)",  ['X','X','X','X','D','X','X','X','X','X','X','X']),
    moduloRow("PRESENTACIÓN EXP.2 (35%)",        ['X','X','X','X','D','D','X','X','X','X','X','X']),
    moduloRow("Dashboard Admin Sede + Validación",['X','X','X','X','X','P','X','X','X','X','X','X']),
    moduloRow("Liberación/devolución pagos",     ['X','X','X','X','X','P','X','X','X','X','X','X']),
    moduloRow("Reagendamiento + Timeout 48h",    ['X','X','X','X','X','A','A','X','X','X','X','X']),
    moduloRow("Panel Admin General",             ['X','X','X','X','X','X','A','A','X','X','X','X']),
    moduloRow("Notificaciones in-app",           ['X','X','X','X','X','X','A','X','X','X','X','X']),
    moduloRow("Reputación básica (reviews)",     ['X','X','X','X','X','X','X','A','A','X','X','X']),
    moduloRow("Mejoras UX",                      ['X','X','X','X','X','X','X','X','_','_','X','X']),
    moduloRow("QA integral + Tests",             ['X','X','X','X','X','X','X','X','X','_','X','X']),
    moduloRow("Correcciones",                    ['X','X','X','X','X','X','X','X','X','_','_','X']),
    moduloRow("Preparación defensa final",       ['X','X','X','X','X','X','X','X','X','X','_','X']),
    moduloRow("DEFENSA FINAL (40%)",             ['X','X','X','X','X','X','X','X','X','X','_','_']),
  ]
});

const leyendaPanorama = [
  p(" "),
  p([new TextRun({ text: "Leyenda:", bold: true })]),
  p([new TextRun({ text: "  ✓ ", bold: true, color: "008000" }), new TextRun("Completado    "),
     new TextRun({ text: "● ", bold: true, color: "B85C00" }), new TextRun("En progreso    "),
     new TextRun({ text: "► ", bold: true, color: "0070C0" }), new TextRun("Adelantado (extra)    "),
     new TextRun("(sin marca) Pendiente")]),
  p([new TextRun({ text: "Verde claro: ", bold: true }), new TextRun("Sprint completado.  "),
     new TextRun({ text: "Amarillo: ", bold: true }), new TextRun("Sprint en curso.  "),
     new TextRun({ text: "Azul claro: ", bold: true }), new TextRun("Implementado antes del sprint planificado.  "),
     new TextRun({ text: "Gris: ", bold: true }), new TextRun("Pendiente.")]),
  p(" ")
];

const panoramaSection = [
  h1("4. Panorama General de la Gantt con Estado"),
  panoramaTable,
  ...leyendaPanorama
];

// === SPRINTS COMPLETADOS - resumen ejecutivo ===
function dodList(items) {
  return items.map(i => bullet(i));
}

const sprintsCompletados = [
  new Paragraph({ children: [new PageBreak()] }),
  h1("5. Sprints 0 a 4 - Completados ✓"),

  h2("Sprint 0 - Documentación de Diseño (22-25 abril) ✓"),
  p([new TextRun({ text: "Entregado el ", italics: true }), new TextRun({ text: "sábado 25 de abril a las 23:00 hrs.", bold: true })]),
  p("Definition of Done verificado:"),
  ...dodList([
    "18 Reglas de Negocio formalizadas",
    "15-20 Historias de Usuario con criterios de aceptación verificables",
    "Diagrama de Casos de Uso (5 actores)",
    "Diagrama de Clases (~15 clases)",
    "3 Diagramas de Actividad (flujos críticos)",
    "2 Diagramas de Secuencia (pago y validación)",
    "Diagrama de Componentes (monolito modular, 6 módulos)",
    "Diagrama de Arquitectura completo",
    "30+ Requisitos Funcionales documentados",
    "15+ Requisitos No Funcionales documentados",
    "Documentación entregada en PDF + GitHub /docs/sprint-0"
  ]),
  p(" "),

  h2("Sprint 1 - Fundaciones (27-abr a 3-may) ✓"),
  p("Sistema corriendo de extremo a extremo en cloud."),
  ...dodList([
    "Repositorio GitHub con estructura modular",
    "Backend Spring Boot 3.2 + Java 21 corriendo en cloud",
    "Frontend Vue 3 + Vite + Tailwind desplegado en cloud",
    "BD PostgreSQL 16 con tablas creadas",
    "Registro y login con JWT funcionando",
    "MER documentado en repo",
    "Mockups de 8 pantallas principales"
  ]),
  p(" "),

  h2("Sprint 2 - Usuarios, Asociados y Sedes (4-10 may) ✓"),
  ...dodList([
    "Edición de perfil y método de devolución preferido",
    "Upload seguro de documentos (Cloudinary/Supabase)",
    "CRUD completo de Asociados (Titular puede crear)",
    "Validación manual de identidad por Admin General",
    "Registro de Sede con upload de fotos",
    "Registro de Sala con características artísticas",
    "Pantalla Admin General para validar identidades y aprobar sedes",
    "5 sedes aprobadas + 20 salas como semilla",
    "Componente ConfirmDialog reutilizable"
  ]),
  p(" "),

  h2("Sprint 3 - Clases, Búsqueda y Carrito (11-17 may) ✓"),
  ...dodList([
    "Entidad Clase con relaciones a Sede y Sala",
    "Maestro Independiente puede reservar sala y crear clase",
    "Sede puede crear clase con Maestro Dependiente",
    "Búsqueda con filtros (disciplina, comuna, fechas, precios, nivel)",
    "Carrito con selección de beneficiario (Titular o Asociado)",
    "Validación de duplicados en carrito",
    "100 clases de prueba distribuidas en BD"
  ]),
  p(" "),

  h2("Sprint 4 - Pago Consolidado + Experiencia 2 (18-24 may) ✓"),
  p([new TextRun({ text: "Hito superado: ", bold: true }), new TextRun("Experiencia 2 (35%) presentada exitosamente.")]),
  ...dodList([
    "Endpoint POST /cart/checkout con transacción atómica",
    "Integración real con MercadoPago (no simulada)",
    "Validación de cupos al pagar",
    "Estado inicial RETENIDO para pagos individuales",
    "Historial de pagos con detalle por asociado",
    "PaymentSuccessPage procesa queryParams de MercadoPago",
    "CartPage con ConfirmModal antes del checkout (R14)",
    "PPT Experiencia 2 ensayado y defendido"
  ]),
];

// === SPRINT 5 - ACTUAL DETALLADO ===
const sprint5Section = [
  new Paragraph({ children: [new PageBreak()] }),
  h1("6. Sprint 5 - Semana Actual (25-31 may 2026)"),
  p([new TextRun({ text: "Objetivo: ", bold: true }), new TextRun("Cerrar Experiencia 2 y avanzar con estados de clase, validación a través del dashboard del Admin Sede y liberación de pagos.")]),
  p([new TextRun({ text: "Estado al 30-may (sábado, día de demo): ", bold: true }), new TextRun({ text: "95% completado", color: "B85C00", bold: true })]),
  p(" "),

  h3("Lo entregado esta semana:"),
  ...dodList([
    "Enum ClassStatus implementado con todas las transiciones",
    "Endpoint asistencia (Maestro) operativo",
    "GET /classes/by-validate para Admin Sede",
    "Endpoints confirmar clase REALIZADA / NO_REALIZADA",
    "Liberación automática de pagos al confirmar REALIZADA",
    "Procesamiento de DEVOLUCION al confirmar NO_REALIZADA",
    "ClassConfirmationService con tests unitarios",
    "Dashboard Admin Sede 'Clases por confirmar'",
    "Pantalla detalle clase con botones de confirmación + dialogs",
    "Pantalla Profesor con asistencia y estado",
    "Pantalla Alumno con estado de cada clase",
    "Trigger de auditoría de cambios de estado en BD",
    "Cancelación de inscripción por Alumno (con DEVOLUCION manual)",
    "Reviews frontend (página de evaluación pendiente)"
  ]),
  p(" "),

  h3("Pendiente para cierre del Sprint 5:"),
  ...dodList([
    "Demo final del sábado (en curso, hoy 30-may)",
    "Documentación del modelo de pagos",
    "Incorporar feedback docente Experiencia 2",
    "Retro del domingo 31-may"
  ]),
];

// === TAREAS EXTRA - NUEVA SECCIÓN CRÍTICA ===
const tareasExtra = [
  new Paragraph({ children: [new PageBreak()] }),
  h1("7. Tareas EXTRA Implementadas (Fuera del Plan Original)"),
  p("Durante el desarrollo, el equipo identificó funcionalidades adicionales que no estaban en el plan original pero resultaron necesarias para cumplir cabalmente con el MVP, las Reglas de Negocio y la usabilidad del producto. Estas son las 27 funcionalidades extra implementadas:"),
  p(" "),

  h2("Backend (10 extras)"),
  ...dodList([
    "Endpoint DELETE /api/classes/{id} para eliminar borradores con validación de propietario",
    "Endpoint POST /api/payments/enrollments/{id}/cancel para cancelación de inscripción por alumno",
    "Endpoint /api/profesor/pagos como alias estructurado de /teacher/earnings",
    "GET /api/profesor/clases/borradores con filtro de DRAFTs del profesor autenticado",
    "POST /api/profesor/clases/borrador para crear borrador sin sala (clase en preparación)",
    "POST /api/profesor/clases/{id}/asignar-reserva para asignar sala a borrador y publicar",
    "Endpoint GET /api/reviews/eligible/student con clases COMPLETED no evaluadas",
    "ProfessionalProfileService.isComplete() para validar perfil mínimo del Maestro",
    "Validación de unicidad de documento de identidad entre usuarios distintos",
    "@EnableScheduling habilitado para procesar timeouts de reagendamiento (Fix #1)"
  ]),
  p(" "),

  h2("Frontend (10 extras)"),
  ...dodList([
    "Nueva página /profesor/clases-por-asignar (DRAFTs con sala asignada)",
    "Sección 'Clases por Asignar' con badge contador en Dashboard Maestro",
    "Modal 'Usar Borrador Existente' en CrearClasePage (alterna a borrador en lugar de nueva clase)",
    "Banner persistente 'Completar Perfil Profesional' en DefaultLayout del contexto Maestro",
    "Redirección automática a /profesor/perfil-profesional?primeraVez=true tras primera reserva",
    "Cambio automático de contexto Alumno → Maestro al hacer reserva (setModo)",
    "Botón 'Agenda tu Sala' del dashboard Alumno con badge 'Contexto Maestro' si ya tiene rol",
    "ProfesionalProfilePage completa con 4 secciones (Disciplina, Presentación, Formación, Redes)",
    "Cancel enrollment desde Mis Clases con modal de confirmación y aviso de reembolso manual",
    "Interceptor api.js detecta atributosActualizados:true y dispara syncAtributos() automático"
  ]),
  p(" "),

  h2("Reglas de Negocio reforzadas (4 extras)"),
  ...dodList([
    "R14 - Confirmación explícita 'confirmacion:true' en teacher-decision del reagendamiento (Fix #3)",
    "R19 - Validación de actor en reagendamiento según TipoClase PROPIA/ASIGNADA (Fix #2)",
    "Rol TEACHER otorgado automáticamente al confirmar reserva de sala (no al crear borrador)",
    "tipoClase ASIGNADA enviado desde SedeCrearClasePage (Fix #6)"
  ]),
  p(" "),

  h2("Tests (3 extras anticipados del Sprint 9)"),
  ...dodList([
    "PaymentServiceTest.java (carrito, duplicados, atomicidad checkout MercadoPago)",
    "ClassConfirmationServiceTest.java (REALIZADA→pagos liberados, NO_REALIZADA→notificaciones R19)",
    "RescheduleServiceTest.java (R19 enforcement, timeout processor, decisión estudiante)"
  ]),
  p(" "),

  p([new TextRun({ text: "Impacto en la carga del proyecto: ", bold: true }),
     new TextRun("estimadas ~80 horas de trabajo adicional ya invertidas, distribuidas entre Sprints 3 a 5. Esto reduce considerablemente el alcance pendiente para los Sprints 6 a 9.")])
];

// === SPRINTS 6-11 REPLANIFICADOS ===
const sprintsFuturos = [
  new Paragraph({ children: [new PageBreak()] }),
  h1("8. Sprints 6 a 11 - Replanificación"),
  p("Dado el adelanto de funcionalidades, los sprints futuros se reorientan hacia consolidación, QA, performance y documentación."),
  p(" "),

  h2("Sprint 6 - Semana 13 (1-7 jun) - Cierre Reagendamiento + Cancelación"),
  p([new TextRun({ text: "Adelantado: ", bold: true }), new TextRun("R19, scheduled tasks, confirmación R14 ya implementados.")]),
  p([new TextRun({ text: "Foco del sprint: ", bold: true })]),
  ...dodList([
    "Algoritmo de sugerencia de fechas según agenda real (mejora del manual actual)",
    "Pantalla Profesor: ver propuesta + fechas sugeridas",
    "Pantalla Alumno: notificación con contador 48h visual",
    "Visualización de timeline de reagendamiento",
    "Cobertura de tests del flujo completo de reagendamiento",
    "Documentación técnica del flujo en /docs/reagendamiento.md"
  ]),
  p(" "),

  h2("Sprint 7 - Semana 14 (8-14 jun) - Panel Admin + Notificaciones"),
  p([new TextRun({ text: "Adelantado: ", bold: true }), new TextRun("Panel Admin General con aprobación de sedes y validación de identidad funcional. Notificaciones in-app parciales.")]),
  p([new TextRun({ text: "Foco del sprint: ", bold: true })]),
  ...dodList([
    "Endpoints suspender/reactivar usuarios con confirmación",
    "Métricas adicionales para dashboard admin (4 visuales con Recharts)",
    "Vistas materializadas para dashboard",
    "Integración completa de notificaciones en todos los eventos clave",
    "Optimización de queries lentas"
  ]),
  p(" "),

  h2("Sprint 8 - Semana 15 (15-21 jun) - Reputación + UX"),
  p([new TextRun({ text: "Adelantado: ", bold: true }), new TextRun("Reviews backend + frontend implementados.")]),
  p([new TextRun({ text: "Foco del sprint: ", bold: true })]),
  ...dodList([
    "Vista materializada con reputación calculada",
    "Trigger que recalcula score promedio",
    "Componente badge de reputación visible en perfil de Maestro y Sede",
    "Datos de prueba con evaluaciones históricas (50+ reviews)",
    "Loading states en todas las pantallas",
    "Mensajes de error claros (RNF)",
    "Responsive móvil del flujo crítico (RNF)",
    "Accesibilidad básica (contraste, tab order)"
  ]),
  p(" "),

  h2("Sprint 9 - Semana 16 (22-28 jun) - QA Integral"),
  p([new TextRun({ text: "Adelantado: ", bold: true }), new TextRun("3 archivos de tests unitarios backend ya creados.")]),
  p([new TextRun({ text: "Foco del sprint: ", bold: true })]),
  ...dodList([
    "Plan de pruebas de aceptación basado en las 20 HU",
    "Ejecución de 20 escenarios del flujo crítico",
    "Tests de integración carrito + pago + estados",
    "Tests de borde: concurrencia, pagos simultáneos",
    "Revisión de seguridad",
    "Pruebas de carga 50 usuarios concurrentes",
    "Revisión responsive en 3 dispositivos reales",
    "Cierre con 0 bugs críticos, máx 3 bugs menores",
    "Cobertura JaCoCo objetivo 60% (RNF-MAN-03)"
  ]),
  p(" "),

  h2("Sprint 10 - Semana 17 (29-jun a 5-jul) - Pulido + Preparación Defensa"),
  ...dodList([
    "Consolidar documentación final",
    "Documentación técnica de arquitectura y despliegue",
    "Scripts finales: BD + datos prueba + P.A.",
    "Video demo de 3 minutos",
    "Ajustes visuales finales",
    "PPT Evaluación Final con cronómetro 40 min",
    "Ensayo 1 y Ensayo 2 con tenida formal",
    "30 preguntas anticipadas con respuestas",
    "Carpetas Docs + Producto organizadas para entrega"
  ]),
  p(" "),

  h2("Sprint 11 - Semana 18 (6-12 jul) - DEFENSA FINAL"),
  p([new TextRun({ text: "Evaluación Final Transversal (40%) ", bold: true, color: "C00000" })]),
  ...dodList([
    "Repaso final del sistema",
    "Ensayo final el día previo",
    "Presentación de Evaluación Final Transversal",
    "Retrospectiva final del proyecto",
    "Documentación de fase futura: roadmap post-proyecto"
  ])
];

// === HITOS CLAVE ===
const hitosTable = new Table({
  width: { size: CONTENT_W, type: WidthType.DXA },
  columnWidths: [720, 1440, 3960, 2880, 1080],
  rows: [
    new TableRow({ tableHeader: true, children: [
      tc("Hito", { width: 720, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
      tc("Fecha", { width: 1440, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
      tc("Descripción", { width: 3960, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
      tc("Criterio de éxito", { width: 2880, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
      tc("Estado", { width: 1080, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER })
    ]}),
    new TableRow({ children: [
      tc("H0", { width: 720, bold: true, fill: COLOR_DONE }),
      tc("25-abr", { width: 1440, fill: COLOR_DONE }),
      tc("ENTREGA DOCUMENTACIÓN DE DISEÑO", { width: 3960, fill: COLOR_DONE }),
      tc("4 entregables: Reglas/HU, UML, Requisitos, Arquitectura", { width: 2880, fill: COLOR_DONE }),
      tc("✓ LOGRADO", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE, color: "008000" })
    ]}),
    new TableRow({ children: [
      tc("H1", { width: 720, bold: true, fill: COLOR_DONE }),
      tc("3-may", { width: 1440, fill: COLOR_DONE }),
      tc("Sistema desplegado en cloud con login funcional", { width: 3960, fill: COLOR_DONE }),
      tc("URL pública + registro y login operativos", { width: 2880, fill: COLOR_DONE }),
      tc("✓ LOGRADO", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE, color: "008000" })
    ]}),
    new TableRow({ children: [
      tc("H2", { width: 720, bold: true, fill: COLOR_DONE }),
      tc("17-may", { width: 1440, fill: COLOR_DONE }),
      tc("Búsqueda y carrito operativos", { width: 3960, fill: COLOR_DONE }),
      tc("Alumno puede buscar, filtrar y agregar al carrito", { width: 2880, fill: COLOR_DONE }),
      tc("✓ LOGRADO", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE, color: "008000" })
    ]}),
    new TableRow({ children: [
      tc("H3", { width: 720, bold: true, fill: COLOR_DONE }),
      tc("Sem 11-12", { width: 1440, fill: COLOR_DONE }),
      tc("PRESENTACIÓN EXPERIENCIA 2 (35%)", { width: 3960, fill: COLOR_DONE, bold: true }),
      tc("Sistema funcional con pago consolidado defendido", { width: 2880, fill: COLOR_DONE }),
      tc("✓ LOGRADO", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_DONE, color: "008000" })
    ]}),
    new TableRow({ children: [
      tc("H4", { width: 720, bold: true, fill: COLOR_EXTRA }),
      tc("7-jun", { width: 1440, fill: COLOR_EXTRA }),
      tc("Reagendamiento operativo", { width: 3960, fill: COLOR_EXTRA }),
      tc("Flujo completo con timeout 48h funcional", { width: 2880, fill: COLOR_EXTRA }),
      tc("► 80% ADELANTADO", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_EXTRA, color: "0070C0" })
    ]}),
    new TableRow({ children: [
      tc("H5", { width: 720, bold: true, fill: COLOR_EXTRA }),
      tc("14-jun", { width: 1440, fill: COLOR_EXTRA }),
      tc("Panel admin y notificaciones", { width: 3960, fill: COLOR_EXTRA }),
      tc("Admin General puede gestionar sistema completo", { width: 2880, fill: COLOR_EXTRA }),
      tc("► 60% ADELANTADO", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_EXTRA, color: "0070C0" })
    ]}),
    new TableRow({ children: [
      tc("H6", { width: 720, bold: true, fill: COLOR_PENDING }),
      tc("28-jun", { width: 1440, fill: COLOR_PENDING }),
      tc("QA completo y sistema estable", { width: 3960, fill: COLOR_PENDING }),
      tc("0 bugs críticos, máximo 3 menores", { width: 2880, fill: COLOR_PENDING }),
      tc("○ PENDIENTE", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_PENDING })
    ]}),
    new TableRow({ children: [
      tc("H7", { width: 720, bold: true, fill: COLOR_PENDING }),
      tc("Sem 17-18", { width: 1440, fill: COLOR_PENDING }),
      tc("EVALUACIÓN FINAL TRANSVERSAL (40%)", { width: 3960, fill: COLOR_PENDING, bold: true }),
      tc("MVP completo defendido", { width: 2880, fill: COLOR_PENDING }),
      tc("○ PENDIENTE", { width: 1080, bold: true, align: AlignmentType.CENTER, fill: COLOR_PENDING })
    ]})
  ]
});

const hitosSection = [
  new Paragraph({ children: [new PageBreak()] }),
  h1("9. Hitos Clave del Proyecto - Actualizados"),
  hitosTable,
  p(" ")
];

// === RIESGOS ===
const riesgosTable = new Table({
  width: { size: CONTENT_W, type: WidthType.DXA },
  columnWidths: [3240, 1080, 1080, 4680],
  rows: [
    new TableRow({ tableHeader: true, children: [
      tc("Riesgo", { width: 3240, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
      tc("Prob.", { width: 1080, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
      tc("Imp.", { width: 1080, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
      tc("Acción / Estado de mitigación", { width: 4680, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER })
    ]}),
    new TableRow({ children: [
      tc("Calidad insuficiente de QA en Sprint 9", { width: 3240, bold: true }),
      tc("Media", { width: 1080, align: AlignmentType.CENTER }),
      tc("Alto", { width: 1080, align: AlignmentType.CENTER }),
      tc("MITIGADO PARCIALMENTE: 3 archivos de tests unitarios backend ya creados, cobertura inicial 30%. Sprint 9 se enfoca en alcanzar el 60% objetivo.", { width: 4680 })
    ]}),
    new TableRow({ children: [
      tc("Bugs derivados de funcionalidades extra", { width: 3240, bold: true }),
      tc("Media", { width: 1080, align: AlignmentType.CENTER }),
      tc("Medio", { width: 1080, align: AlignmentType.CENTER }),
      tc("Mitigación: revisión cruzada al final de cada sprint. Sprint 9 dedicado a QA integral incluye escenarios sobre todas las funcionalidades implementadas (planificadas y extra).", { width: 4680 })
    ]}),
    new TableRow({ children: [
      tc("Defensa final con cronómetro 40 min", { width: 3240, bold: true }),
      tc("Media", { width: 1080, align: AlignmentType.CENTER }),
      tc("Alto", { width: 1080, align: AlignmentType.CENTER }),
      tc("Sprint 10 dedica 2 ensayos completos. 30 preguntas anticipadas preparadas. Video demo de 3 min como respaldo.", { width: 4680 })
    ]}),
    new TableRow({ children: [
      tc("Parciales de otras asignaturas", { width: 3240, bold: true }),
      tc("Alta", { width: 1080, align: AlignmentType.CENTER }),
      tc("Medio", { width: 1080, align: AlignmentType.CENTER }),
      tc("Holgura ganada por adelantos permite ceder 30% de carga en semanas con parciales sin comprometer la entrega.", { width: 4680 })
    ]}),
    new TableRow({ children: [
      tc("Problemas con despliegue cloud", { width: 3240, bold: true }),
      tc("Baja", { width: 1080, align: AlignmentType.CENTER }),
      tc("Alto", { width: 1080, align: AlignmentType.CENTER }),
      tc("MITIGADO: despliegue validado desde Sprint 1, CI/CD funcionando, ambiente backup local mediante Docker Compose.", { width: 4680 })
    ]}),
    new TableRow({ children: [
      tc("Enfermedad o ausencia de un integrante", { width: 3240, bold: true }),
      tc("Media", { width: 1080, align: AlignmentType.CENTER }),
      tc("Alto", { width: 1080, align: AlignmentType.CENTER }),
      tc("Mitigación activa: cada módulo ha sido tocado por al menos 2 integrantes vía pair programming. Documentación interna actualizada.", { width: 4680 })
    ]})
  ]
});

const riesgosSection = [
  new Paragraph({ children: [new PageBreak()] }),
  h1("10. Gestión de Riesgos del Cronograma - Actualizado"),
  p("Los riesgos del Sprint 0 (entrega de documentación en 3 días) se materializaron pero fueron mitigados con éxito. Los riesgos activos al 30-may-2026 son:"),
  p(" "),
  riesgosTable,
  p(" ")
];

// === CAPACIDAD ===
const capacidadTable = new Table({
  width: { size: CONTENT_W, type: WidthType.DXA },
  columnWidths: [3960, 2160, 3960],
  rows: [
    new TableRow({ tableHeader: true, children: [
      tc("Métrica", { width: 3960, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
      tc("Valor", { width: 2160, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER }),
      tc("Observación", { width: 3960, bold: true, fill: COLOR_HEADER, color: COLOR_HEADER_TEXT, align: AlignmentType.CENTER })
    ]}),
    new TableRow({ children: [
      tc("Horas consumidas al 30-may", { width: 3960, bold: true }),
      tc("~450 hrs", { width: 2160, bold: true, align: AlignmentType.CENTER, color: "0070C0" }),
      tc("Sprint 0 (58h) + Sprints 1-5 (~390h)", { width: 3960 })
    ]}),
    new TableRow({ children: [
      tc("Horas restantes planificadas", { width: 3960 }),
      tc("~435 hrs", { width: 2160, align: AlignmentType.CENTER }),
      tc("Sprints 6-11", { width: 3960 })
    ]}),
    new TableRow({ children: [
      tc("Capacidad disponible restante", { width: 3960 }),
      tc("450 hrs", { width: 2160, align: AlignmentType.CENTER }),
      tc("6 semanas × 75 hrs/equipo/semana", { width: 3960 })
    ]}),
    new TableRow({ children: [
      tc("Holgura proyectada al cierre", { width: 3960, bold: true }),
      tc("~15 hrs", { width: 2160, bold: true, align: AlignmentType.CENTER, color: "008000" }),
      tc("Tras absorber ~80h de funcionalidades extra ya implementadas", { width: 3960 })
    ]}),
    new TableRow({ children: [
      tc("% MVP completado", { width: 3960, bold: true }),
      tc("~70%", { width: 2160, bold: true, align: AlignmentType.CENTER, color: "008000" }),
      tc("Núcleo funcional + módulos extra operativos", { width: 3960 })
    ]}),
    new TableRow({ children: [
      tc("Sprints completados", { width: 3960 }),
      tc("5 de 12", { width: 2160, align: AlignmentType.CENTER }),
      tc("Sprint 0 + Sprints 1-4 + Sprint 5 en cierre", { width: 3960 })
    ]}),
    new TableRow({ children: [
      tc("Hitos de evaluación cumplidos", { width: 3960 }),
      tc("2 de 3", { width: 2160, align: AlignmentType.CENTER }),
      tc("Sprint 0 (25-abr) ✓ + Exp.2 (35%) ✓ · Pendiente: Final (40%)", { width: 3960 })
    ]})
  ]
});

const capacidadSection = [
  new Paragraph({ children: [new PageBreak()] }),
  h1("11. Resumen de Capacidad y Carga - Actualizado"),
  p("Análisis al cierre del Sprint 5 (30-may-2026):"),
  p(" "),
  capacidadTable,
  p(" "),
  p([new TextRun({ text: "Conclusión: ", bold: true }), new TextRun("El proyecto se encuentra en una posición saludable. La holgura proyectada permite absorber imprevistos, dedicar tiempo a pulido visual de UX y reforzar la cobertura de tests para alcanzar la meta del 60% (RNF-MAN-03).")])
];

// === CIERRE ===
const cierre = [
  p(" "),
  p(" "),
  p([new TextRun({ text: "Carta Gantt actualizada al 30 de mayo de 2026. ", bold: true }),
     new TextRun("Elaborada en base al MVP formal acordado por el equipo, la metodología Scrum indicada por el docente, la capacidad real de dedicación del equipo y el progreso real verificado mediante repositorio GitHub e instancias desplegadas.")]),
  p([new TextRun({ text: "Próxima revisión sugerida: ", italics: true }), new TextRun({ text: "viernes 6 de junio de 2026, al cierre del Sprint 6.", italics: true })])
];

// === DOCUMENT ===
const doc = new Document({
  creator: "Equipo Modo Ensayo",
  title: "Carta Gantt Actualizada - Modo Ensayo",
  description: "Actualización al 30-may-2026 (fin Sprint 5)",
  styles: {
    default: { document: { run: { font: "Arial", size: 22 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 32, bold: true, font: "Arial", color: "2E75B6" },
        paragraph: { spacing: { before: 360, after: 200 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 26, bold: true, font: "Arial", color: "1F4E79" },
        paragraph: { spacing: { before: 240, after: 120 }, outlineLevel: 1 } },
      { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 22, bold: true, font: "Arial", color: "404040" },
        paragraph: { spacing: { before: 180, after: 100 }, outlineLevel: 2 } }
    ]
  },
  numbering: {
    config: [{
      reference: "bullets",
      levels: [{
        level: 0,
        format: LevelFormat.BULLET,
        text: "•",
        alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 540, hanging: 270 } } }
      }]
    }]
  },
  sections: [{
    properties: { page: PAGE },
    headers: {
      default: new Header({ children: [
        p([new TextRun({ text: "Carta Gantt Actualizada · Modo Ensayo", size: 18, color: "808080" })], { align: AlignmentType.RIGHT })
      ]})
    },
    footers: {
      default: new Footer({ children: [
        p([new TextRun({ text: "Equipo: Darlette Morales · Jonathan Guerra · Victor Silva    |    ", size: 18, color: "808080" }),
           new TextRun({ text: "Página ", size: 18, color: "808080" }),
           new TextRun({ children: [PageNumber.CURRENT], size: 18, color: "808080" })], { align: AlignmentType.CENTER })
      ]})
    },
    children: [
      ...portada,
      infoTable,
      ...estadoGeneral,
      resumenProgresoTable,
      ...resumenEjecutivo,
      ...equipoSection,
      ...panoramaSection,
      ...sprintsCompletados,
      ...sprint5Section,
      ...tareasExtra,
      ...sprintsFuturos,
      ...hitosSection,
      ...riesgosSection,
      ...capacidadSection,
      ...cierre
    ]
  }]
});

Packer.toBuffer(doc).then(buffer => {
  const outPath = path.join(__dirname, "Carta_Gantt_Actualizada_30may2026.docx");
  fs.writeFileSync(outPath, buffer);
  console.log("OK -> " + outPath);
  console.log("Size: " + buffer.length + " bytes");
});
