import java.awt.*
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.abs
import kotlin.math.roundToInt

class ColorConverter {
    // Преобразование RGB в CMYK
    fun rgbToCmyk(r: Int, g: Int, b: Int): List<Double> {
        if (listOf(r, g, b).all { it == 0 }) {
            return listOf(0.0, 0.0, 0.0, 1.0)
        }
        val rNorm = r / 255.0
        val gNorm = g / 255.0
        val bNorm = b / 255.0

        val k = 1 - maxOf(rNorm, gNorm, bNorm)
        val c = (1 - rNorm - k) / (1 - k)
        val m = (1 - gNorm - k) / (1 - k)
        val y = (1 - bNorm - k) / (1 - k)

        return listOf(c, m, y, k)
    }

    // Преобразование CMYK в RGB
    fun cmykToRgb(c: Double, m: Double, y: Double, k: Double): List<Int> {
        val r = (255 * (1 - c) * (1 - k)).roundToInt()
        val g = (255 * (1 - m) * (1 - k)).roundToInt()
        val b = (255 * (1 - y) * (1 - k)).roundToInt()

        return listOf(r, g, b)
    }

    // Преобразование RGB в HSV
    fun rgbToHsv(r: Int, g: Int, b: Int): List<Double> {
        val rNorm = r / 255.0
        val gNorm = g / 255.0
        val bNorm = b / 255.0

        val max = maxOf(rNorm, gNorm, bNorm)
        val min = minOf(rNorm, gNorm, bNorm)
        val delta = max - min

        val v = max
        val s = if (max != 0.0) delta / max else 0.0

        var h = 0.0
        if (delta != 0.0) {
            when (max) {
                rNorm -> h = (gNorm - bNorm) / delta
                gNorm -> h = 2.0 + (bNorm - rNorm) / delta
                bNorm -> h = 4.0 + (rNorm - gNorm) / delta
            }
            h = (h * 60) % 360
            if (h < 0) h += 360
        }

        return listOf(h, s * 100, v * 100)
    }

    // Преобразование HSV в RGB
    fun hsvToRgb(h: Double, s: Double, v: Double): List<Int> {
        val sNorm = s / 100
        val vNorm = v / 100

        val c = vNorm * sNorm
        val x = c * (1 - abs((h / 60) % 2 - 1))
        val m = vNorm - c

        val (r1, g1, b1) = when {
            h < 60 -> listOf(c, x, 0.0)
            h < 120 -> listOf(x, c, 0.0)
            h < 180 -> listOf(0.0, c, x)
            h < 240 -> listOf(0.0, x, c)
            h < 300 -> listOf(x, 0.0, c)
            else -> listOf(c, 0.0, x)
        }

        val r = ((r1 + m) * 255).roundToInt()
        val g = ((g1 + m) * 255).roundToInt()
        val b = ((b1 + m) * 255).roundToInt()

        return listOf(r, g, b)
    }
}


enum class ColorModel {
    RGB,
    CMYK,
    HSV
}

class ColorConverterApp : JFrame("Конвертер цветов") {
    private val colorConverter = ColorConverter()
    private var isUpdating = false

    private var rgbValues = listOf(0, 0, 0)
    private var hsvValues = listOf(0.0, 0.0, 0.0)
    private var cmykValues = listOf(0.0, 0.0, 0.0, 1.0)

    private val rgbFields = rgbValues.map { JTextField(5) }
    private val cmykFields = cmykValues.map { JTextField(5) }
    private val hsvFields = hsvValues.map { JTextField(5) }

    private val rgbSliders = List(3) { createSlider(0, 255) }
    private val cmykSliders = List(4) { createSlider(0, 100) }
    private val hsvSliders = listOf(createSlider(0, 360), createSlider(0, 100), createSlider(0, 100))

    private val colorDisplayPanel = JPanel().apply {
        preferredSize = Dimension(400, 300)
        background = Color.BLACK
    }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = GridBagLayout()
        val gbc = GridBagConstraints().apply {
            insets = Insets(10, 10, 10, 10) // Отступы
            fill = GridBagConstraints.HORIZONTAL
        }

        val rgbPanel = createColorPanel("RGB:", rgbFields, rgbSliders)
        gbc.gridx = 0
        gbc.gridy = 0
        add(rgbPanel, gbc)

        val cmykPanel = createColorPanel("CMYK:", cmykFields, cmykSliders)
        gbc.gridx = 0
        gbc.gridy = 1
        add(cmykPanel, gbc)

        val hsvPanel = createColorPanel("HSV:", hsvFields, hsvSliders)
        gbc.gridx = 0
        gbc.gridy = 2
        add(hsvPanel, gbc)

        gbc.gridx = 0
        gbc.gridy = 3
        add(colorDisplayPanel, gbc)

        val colorChooserButton = JButton("Выбрать цвет").apply {
            addActionListener { chooseColor() }
        }
        gbc.gridx = 0
        gbc.gridy = 4
        add(colorChooserButton, gbc)

        rgbFields.forEach { addDocumentListener(it, ColorModel.RGB) }
        cmykFields.forEach { addDocumentListener(it, ColorModel.CMYK) }
        hsvFields.forEach { addDocumentListener(it, ColorModel.HSV) }

        rgbSliders.forEachIndexed { i, slider -> addSliderListener(slider, rgbFields[i], ColorModel.RGB) }
        cmykSliders.forEachIndexed { i, slider -> addSliderListener(slider, cmykFields[i], ColorModel.CMYK) }
        hsvSliders.forEachIndexed { i, slider -> addSliderListener(slider, hsvFields[i], ColorModel.HSV) }

        updateFields()
        updateSliders()
        updateColorDisplay()

        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }

    private fun createColorPanel(title: String, fields: List<JTextField>, sliders: List<JSlider>): JPanel {
        val panel = JPanel().apply {
            layout = GridBagLayout()
            border = BorderFactory.createTitledBorder(title)
            val gbc = GridBagConstraints().apply {
                insets = Insets(5, 5, 5, 5)
                fill = GridBagConstraints.HORIZONTAL
            }

            fields.forEachIndexed { i, field ->
                gbc.gridx = i
                add(field, gbc)
            }

            sliders.forEachIndexed { i, slider ->
                gbc.gridx = i
                gbc.gridy = 1
                add(slider, gbc)
            }
        }
        return panel
    }

    private fun chooseColor() {
        val color = JColorChooser.showDialog(this, "Выбор цвета", null)
        if (color != null) {
            rgbValues = listOf(color.red, color.green, color.blue)
            updateFromRgb()
            updateFields()
            updateSliders()
            updateColorDisplay()
        }
    }

    private fun addDocumentListener(field: JTextField, model: ColorModel) {
        field.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateColors(model)
            override fun removeUpdate(e: DocumentEvent?) = updateColors(model)
            override fun changedUpdate(e: DocumentEvent?) = updateColors(model)
        })
    }

    private fun addSliderListener(slider: JSlider, field: JTextField, model: ColorModel) {
        slider.addChangeListener {
            if (!isUpdating) {
                when (model) {
                    ColorModel.RGB -> field.text = slider.value.toString()
                    ColorModel.CMYK -> field.text = (slider.value / 100.0).toString()  // Преобразуем значение слайдера в доли для CMYK
                    ColorModel.HSV -> field.text = slider.value.toString()
                }
            }
        }
    }

    private fun createSlider(min: Int, max: Int): JSlider {
        return JSlider(min, max).apply {
            majorTickSpacing = (max - min) / 5
            paintTicks = true
            paintLabels = true
        }
    }

    // Обновление цветовых значений при изменении модели
    private fun updateColors(model: ColorModel) {
        if (isUpdating) return
        SwingUtilities.invokeLater {
            isUpdating = true
            try {
                when (model) {
                    ColorModel.RGB -> {
                        if (rgbFields.any { it.text.isEmpty() }) return@invokeLater
                        val (r, g, b) = rgbFields.map { it.text.toInt() }
                        rgbValues = listOf(r, g, b)
                        updateFromRgb() // Обновляем HSV и CMYK
                        updateCMYKFields()
                        updateHSVFields()
                    }

                    ColorModel.CMYK -> {
                        if (cmykFields.any { it.text.isEmpty() }) return@invokeLater
                        // Используем значения в процентах
                        val (c, m, y, k) = cmykFields.map { it.text.toDouble() / 100 }
                        val (r, g, b) = colorConverter.cmykToRgb(c, m, y, k)
                        rgbValues = listOf(r, g, b)
                        cmykValues = listOf(c, m, y, k)
                        hsvValues = colorConverter.rgbToHsv(r, g, b)
                        updateRGBFields()
                        updateHSVFields()
                    }

                    ColorModel.HSV -> {
                        if (hsvFields.any { it.text.isEmpty() }) return@invokeLater
                        var (h, s, v) = hsvFields.map { it.text.toDouble() }
                        val (r, g, b) = colorConverter.hsvToRgb(h, s, v)
                        rgbValues = listOf(r, g, b)
                        cmykValues = colorConverter.rgbToCmyk(r, g, b)
                        hsvValues = listOf(h, s, v)
                        updateRGBFields()
                        updateCMYKFields()
                    }
                }
                updateSliders()  // Обновляем слайдеры после всех изменений
                updateColorDisplay()
            } catch (e: NumberFormatException) {
                println(e.toString())
            } finally {
                isUpdating = false
            }
        }
    }

    // Обновление полей RGB
    private fun updateRGBFields() {
        updateFields(rgbFields, rgbValues)  // Вызов с текущими значениями для RGB
    }

    // Обновление полей CMYK
    private fun updateCMYKFields() {
        // Преобразуем значения в проценты для отображения в полях
        updateFields(cmykFields, cmykValues.map { (it * 100).toInt() })
    }

    // Обновление полей HSV
    private fun updateHSVFields() {
        val (h, s, v) = hsvValues
        updateFields(hsvFields, listOf(h, s.toInt(), v.toInt()))
    }

    // Перегрузка метода updateFields без параметров
    private fun updateFields() {
        updateFields(rgbFields, rgbValues)  // Вызов с текущими значениями для RGB
        updateFields(cmykFields, cmykValues.map { (it * 100).toInt() })  // Вызов с текущими значениями для CMYK
        updateFields(
            hsvFields,
            listOf(hsvValues[0], (hsvValues[1] * 100).toInt(), (hsvValues[2] * 100).toInt())
        )  // Вызов с текущими значениями для HSV
    }

    // Обновление полей с параметрами 'fields' и 'values'
    private fun updateFields(fields: List<JTextField>, values: List<Number>) {
        fields.forEachIndexed { i, field ->
            field.text = String.format(
                if (values[i] is Double) "%.1f" else "%d", // Если значение Double, показываем 1 знак после запятой, если Int - без десятичных
                values[i]
            )
        }
    }

    private fun updateColorDisplay() {
        val (r, g, b) = rgbValues
        colorDisplayPanel.background = Color(r, g, b)
        colorDisplayPanel.repaint()
    }

    private fun updateSliders() {
        val (h, s, v) = hsvValues
        // Обновление слайдеров для RGB
        mapOf(
            rgbSliders to rgbValues,
            cmykSliders to cmykValues.map { (it * 100).toInt() },  // Преобразуем в проценты для CMYK
            hsvSliders to listOf(h, s, v)
        ).forEach { (sliders, values) ->
            sliders.forEachIndexed { i, slider ->
                slider.value = values[i].toInt()
            }
        }
    }

    private fun updateFromRgb() {
        val (r, g, b) = rgbValues
        cmykValues = colorConverter.rgbToCmyk(r, g, b)
        hsvValues = colorConverter.rgbToHsv(r, g, b)
    }
}


fun main() {
    Locale.setDefault(Locale.ENGLISH)
    SwingUtilities.invokeLater {
        ColorConverterApp()
    }
}

