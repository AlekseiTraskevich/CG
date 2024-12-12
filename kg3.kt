import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import kotlin.math.*
import kotlin.system.measureTimeMillis

class RasterizationApp : JFrame("Растеризация") {
    private val canvas: JPanel = object : JPanel() {
        init {
            isDoubleBuffered = true
            preferredSize = Dimension(500, 500)
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            drawGrid(g)
            drawAxis(g)
            rasterizeLine(g)
        }
    }

    private var scale = 20
    private val coordFields = List(4) { JTextField("0") }
    private val coordLabels = List(4) { i -> JLabel("${if (i % 2 == 0) "x" else "y"}${i / 2}") }
    private val scaleSlider = JSlider(10, 100).apply {
        majorTickSpacing = 20
        paintTicks = true
        paintLabels = true
        value = scale
    }

    private val algoButtons = listOf(
        JRadioButton("Алгоритм шагов", true),
        JRadioButton("Алгоритм DDA"),
        JRadioButton("Алгоритм Брезенхема (Прямые)"),
        JRadioButton("Алгоритм Брезенхема (Круги)").apply {
            addItemListener {
                if (it.stateChange == ItemEvent.SELECTED) {
                    coordFields[3].isVisible = false
                    coordLabels[2].text = "R"
                    coordLabels[3].isVisible = false
                } else {
                    coordFields[3].isVisible = true
                    coordLabels[2].text = "x1"
                    coordLabels[3].isVisible = true
                }
            }
        }
    )

    private val algos = listOf(
        ::stepByStepAlgorithm,
        ::ddaAlgorithm,
        ::bresenhamAlgorithmLine,
        ::bresenhamAlgorithmCircle
    )

    private val output = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        background = Color(200, 200, 200)
        foreground = Color.BLACK
    }

    private var centerX = 0
    private var centerY = 0
    private var doLogging = false

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1000, 800)
        setLocationRelativeTo(null)
        layout = BorderLayout()

        val controlPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(205, 205, 205)

            val algoGroup = ButtonGroup()
            algoButtons.forEach {
                it.background = Color(245, 245, 245)
                add(it)
                algoGroup.add(it)
            }
            add(Box.createVerticalStrut(10))

            val coordPanel = JPanel().apply {
                layout = GridLayout(4, 2, 10, 10)  // Добавлены отступы для компонентов
                coordLabels.zip(coordFields).forEach { (label, field) ->
                    label.foreground = Color.BLACK
                    add(label)
                    add(field)
                }
            }
            add(coordPanel)

            add(Box.createVerticalStrut(10))

            val scalePanel = JPanel().apply {
                layout = FlowLayout(FlowLayout.LEFT)
                add(JLabel("Масштаб:")).apply { foreground = Color.BLACK }
                add(scaleSlider)
            }
            add(scalePanel)

            add(Box.createVerticalStrut(10))

            val drawButton = JButton("Нарисовать").apply {
                preferredSize = Dimension(150, 30)
                background = Color(100, 150, 255) // Синий фон для кнопки
                foreground = Color.WHITE // Белый текст
                addActionListener {
                    try {
                        scale = scaleSlider.value
                        canvas.repaint()
                    } catch (ex: NumberFormatException) {
                        output.text = "Некорректный размер ячейки!"
                    }
                }
            }
            add(drawButton)

            add(Box.createVerticalStrut(10))

            val loggingCheckBox = JCheckBox("Логирование").apply {
                addItemListener {
                    doLogging = it.stateChange == ItemEvent.SELECTED
                }
            }
            add(loggingCheckBox)

            add(Box.createVerticalStrut(10))
        }

        val scrollPane = JScrollPane(output).apply {
            preferredSize = Dimension(600, 120)
        }

        add(controlPanel, BorderLayout.NORTH)
        add(canvas, BorderLayout.CENTER)
        add(scrollPane, BorderLayout.SOUTH)
        isVisible = true
    }

    fun drawGrid(g: Graphics) {
        g.color = Color.LIGHT_GRAY
        var i = 0
        while (i < canvas.width) {
            g.drawLine(i, 0, i, canvas.height)
            i += scale
        }
        i = 0
        while (i < canvas.height) {
            g.drawLine(0, i, canvas.width, i)
            i += scale
        }
    }

    fun drawAxis(g: Graphics) {
        g.color = Color.BLACK
        val width = canvas.width
        val height = canvas.height
        centerX = width / 2 / scale * scale
        centerY = height / 2 / scale * scale
        g.drawLine(centerX, 0, centerX, height)
        g.drawLine(0, centerY, width, centerY)
        g.drawString("X", width - 20, centerY - 10)
        g.drawString("Y", centerX + 10, 20)

        var i = 0
        while (i <= width / 2 - 2 * scale) {
            g.drawLine(centerX + i, centerY - 5, centerX + i, centerY + 5)
            if (i != 0) {
                g.drawString((i / scale).toString(), centerX + i - 5, centerY + 20)
            }
            g.drawLine(centerX - i, centerY - 5, centerX - i, centerY + 5)
            if (i != 0) {
                g.drawString((-i / scale).toString(), centerX - i - 10, centerY + 20)
            }
            i += scale
        }

        i = 0
        while (i <= height / 2 - 2 * scale) {
            g.drawLine(centerX - 5, centerY - i, centerX + 5, centerY - i)
            if (i != 0) {
                g.drawString((i / scale).toString(), centerX + 10, centerY - i + 5)
            }
            g.drawLine(centerX - 5, centerY + i, centerX + 5, centerY + i)
            if (i != 0) {
                g.drawString((-i / scale).toString(), centerX + 10, centerY + i + 5)
            }
            i += scale
        }
    }

    private fun getParams(): List<Int> {
        return if (!algoButtons[3].isSelected) {
            coordFields.map { it.text.toInt() }
        } else {
            coordFields.dropLast(1).map { it.text.toInt() }
        }
    }

    private fun logDraw(x: Int, y: Int) = output.append("Рисуем ($x, $y)\n")

    fun rasterizeLine(g: Graphics) {
        g.color = Color.GREEN
        val id = algoButtons.indexOfFirst { it.isSelected }
        output.text = "${algoButtons[id].text}:\n"
        val time = measureTimeMillis {
            algos[id](g)
        }
        output.append("Время: $time мс\n")
    }

    private fun stepByStepAlgorithm(g: Graphics) {
        var (x0, y0, x1, y1) = getParams()
        if (x0 > x1) {
            x0 = x1.also { x1 = x0 }
        }
        if (y0 > y1) {
            y0 = y1.also { y1 = y0 }
        }
        val dx = x1 - x0
        val dy = y1 - y0
        if (dx > dy) {
            val slope = dy.toDouble() / dx
            for (x in x0..x1) {
                val y = slope * (x - x0) + y0
                drawPixel(g, x, y.toInt())
            }
        } else {
            val slope = dx.toDouble() / dy
            for (y in y0..y1) {
                val x = slope * (y - y0) + x0
                drawPixel(g, x.toInt(), y)
            }
        }
    }

    private fun ddaAlgorithm(g: Graphics) {
        var (x0, y0, x1, y1) = getParams()
        val dx = x1 - x0
        val dy = y1 - y0
        val steps = maxOf(abs(dx), abs(dy))
        val xInc = dx.toDouble() / steps
        val yInc = dy.toDouble() / steps
        var x = x0.toDouble()
        var y = y0.toDouble()
        for (i in 0..steps) {
            drawPixel(g, x.roundToInt(), y.roundToInt())
            x += xInc
            y += yInc
        }
    }

    private fun bresenhamAlgorithmLine(g: Graphics) {
        var (x0, y0, x1, y1) = getParams()
        val dx = x1 - x0
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy
        while (true) {
            drawPixel(g, x0, y0)
            if (x0 == x1 && y0 == y1) break
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x0 += sx
            }
            if (e2 < dx) {
                err += dx
                y0 += sy
            }
        }
    }

    private fun bresenhamAlgorithmCircle(g: Graphics) {
        val (xc, yc, r) = getParams()
        var x = 0
        var y = r
        var d = 3 - 2 * y
        while (x <= y) {
            drawCirclePixels(g, xc, yc, x, y)
            x++
            d = if (d > 0) {
                d + 4 * (x - y--) + 10
            } else {
                d + 4 * x + 6
            }
        }
    }

    private fun drawCirclePixels(g: Graphics, xc: Int, yc: Int, x: Int, y: Int) {
        for (d2 in listOf(1, -1)) {
            for (d1 in listOf(1, -1)) {
                drawPixel(g, xc + x * d1, yc + y * d2)
                drawPixel(g, xc + y * d1, yc + x * d2)
            }
        }
    }

    private fun drawPixel(g: Graphics, x: Int, y: Int) {
        if (doLogging) {
            logDraw(x, y)
        }
        g.fillRect(x * scale + centerX, -y * scale + centerY - scale, scale, scale)
    }
}

fun main() {
    SwingUtilities.invokeLater {
        RasterizationApp()
    }
}
