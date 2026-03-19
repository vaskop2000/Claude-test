package com.musicnotation.editor.data.model

enum class InstrumentFamily(val displayName: String) {
    KEYBOARD("Клавишные"),
    STRINGS("Струнные"),
    WOODWIND("Деревянные духовые"),
    BRASS("Медные духовые"),
    PERCUSSION("Ударные"),
    VOCAL("Вокал"),
    OTHER("Другие")
}

enum class Instrument(
    val displayName: String,
    val family: InstrumentFamily,
    val defaultClef: Clef,
    val secondClef: Clef? = null  // for instruments using multiple staves
) {
    // Keyboard
    PIANO_TREBLE("Фортепиано (верхний)", InstrumentFamily.KEYBOARD, Clef.TREBLE),
    PIANO_BASS("Фортепиано (нижний)", InstrumentFamily.KEYBOARD, Clef.BASS),
    ORGAN_TREBLE("Орган (правая рука)", InstrumentFamily.KEYBOARD, Clef.TREBLE),
    ORGAN_BASS("Орган (левая рука)", InstrumentFamily.KEYBOARD, Clef.BASS),
    HARPSICHORD("Клавесин", InstrumentFamily.KEYBOARD, Clef.TREBLE),

    // Strings
    VIOLIN("Скрипка", InstrumentFamily.STRINGS, Clef.TREBLE),
    VIOLA("Альт", InstrumentFamily.STRINGS, Clef.ALTO),
    CELLO("Виолончель", InstrumentFamily.STRINGS, Clef.BASS),
    DOUBLE_BASS("Контрабас", InstrumentFamily.STRINGS, Clef.BASS),
    GUITAR("Гитара", InstrumentFamily.STRINGS, Clef.TREBLE),
    HARP("Арфа", InstrumentFamily.STRINGS, Clef.TREBLE),

    // Woodwind
    FLUTE("Флейта", InstrumentFamily.WOODWIND, Clef.TREBLE),
    OBOE("Гобой", InstrumentFamily.WOODWIND, Clef.TREBLE),
    CLARINET("Кларнет", InstrumentFamily.WOODWIND, Clef.TREBLE),
    BASSOON("Фагот", InstrumentFamily.WOODWIND, Clef.BASS),
    SAXOPHONE("Саксофон", InstrumentFamily.WOODWIND, Clef.TREBLE),

    // Brass
    HORN("Валторна", InstrumentFamily.BRASS, Clef.TREBLE),
    TRUMPET("Труба", InstrumentFamily.BRASS, Clef.TREBLE),
    TROMBONE("Тромбон", InstrumentFamily.BRASS, Clef.BASS),
    TUBA("Туба", InstrumentFamily.BRASS, Clef.BASS),

    // Percussion
    TIMPANI("Литавры", InstrumentFamily.PERCUSSION, Clef.BASS),
    DRUMS("Ударная установка", InstrumentFamily.PERCUSSION, Clef.TREBLE),
    XYLOPHONE("Ксилофон", InstrumentFamily.PERCUSSION, Clef.TREBLE),

    // Vocal
    SOPRANO("Сопрано", InstrumentFamily.VOCAL, Clef.TREBLE),
    MEZZO_SOPRANO("Меццо-сопрано", InstrumentFamily.VOCAL, Clef.TREBLE),
    ALTO("Альт (голос)", InstrumentFamily.VOCAL, Clef.TREBLE),
    TENOR("Тенор", InstrumentFamily.VOCAL, Clef.TREBLE),
    BARITONE("Баритон", InstrumentFamily.VOCAL, Clef.BASS),
    BASS("Бас", InstrumentFamily.VOCAL, Clef.BASS),

    // Generic
    GENERIC_TREBLE("Общий (скрипичный)", InstrumentFamily.OTHER, Clef.TREBLE),
    GENERIC_BASS("Общий (басовый)", InstrumentFamily.OTHER, Clef.BASS)
}
