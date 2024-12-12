import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;

fun main() {
    SwingUtilities.invokeLater { ImageProcessingApp() }
}

class ImageProcessingApp : JFrame("Морфологическая обработка и фильтры") {
    private val imageLabel = JLabel()
    private var originalImage: BufferedImage? = null

    init {
        setupUI()
        loadImage()
        isVisible = true
    }

    private fun setupUI() {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(800, 600)
        layout = BorderLayout()

        val methodSelectPanel = JPanel().apply {
            layout = FlowLayout()
            val methods = arrayOf("Эрозия", "Дилатация", "Увеличение резкости")
            val methodComboBox = JComboBox(methods)

            val applyButton = JButton("Применить").apply {
                addActionListener {
                    val selectedMethod = methodComboBox.selectedItem as String
                    applyProcessingMethod(selectedMethod)
                }
            }

            add(JLabel("Метод:"))
            add(methodComboBox)
            add(applyButton)
        }

        add(methodSelectPanel, BorderLayout.NORTH)
        add(JScrollPane(imageLabel), BorderLayout.CENTER)
    }

    private fun loadImage() {
        originalImage = ImageIO.read(javaClass.getResource("image.jpg"))
        imageLabel.icon = ImageIcon(originalImage)
    }

    private fun applyProcessingMethod(method: String) {
        val processedImage = when (method) {
            "Эрозия" -> applyErosion(originalImage!!)
            "Дилатация" -> applyDilation(originalImage!!)
            "Увеличение резкости" -> applySharpening(originalImage!!)
            else -> originalImage
        }
        imageLabel.icon = ImageIcon(processedImage)
    }

    private fun applyErosion(image: BufferedImage): BufferedImage {
        val radius = 1
        val result = BufferedImage(image.width, image.height, image.type)

        for (y in radius until image.height - radius) {
            for (x in radius until image.width - radius) {
                var minValue = 255
                for (j in -radius..radius) {
                    for (i in -radius..radius) {
                        val color = Color(image.getRGB(x + i, y + j))
                        val gray = (color.red + color.green + color.blue) / 3
                        if (gray < minValue) {
                            minValue = gray
                        }
                    }
                }
                val newColor = Color(minValue, minValue, minValue)
                result.setRGB(x, y, newColor.rgb)
            }
        }
        return result
    }

    private fun applyDilation(image: BufferedImage): BufferedImage {
        val radius = 1
        val result = BufferedImage(image.width, image.height, image.type)

        for (y in radius until image.height - radius) {
            for (x in radius until image.width - radius) {
                var maxValue = 0
                for (j in -radius..radius) {
                    for (i in -radius..radius) {
                        val color = Color(image.getRGB(x + i, y + j))
                        val gray = (color.red + color.green + color.blue) / 3
                        if (gray > maxValue) {
                            maxValue = gray
                        }
                    }
                }
                val newColor = Color(maxValue, maxValue, maxValue)
                result.setRGB(x, y, newColor.rgb)
            }
        }
        return result
    }

    private fun applySharpening(image: BufferedImage): BufferedImage {
        val kernel = arrayOf(
            intArrayOf(0, -1, 0),
            intArrayOf(-1, 5, -1),
            intArrayOf(0, -1, 0)
        )
        val result = BufferedImage(image.width, image.height, image.type)

        for (y in 1 until image.height - 1) {
            for (x in 1 until image.width - 1) {
                var redSum = 0
                var greenSum = 0
                var blueSum = 0

                for (j in kernel.indices) {
                    for (i in kernel[j].indices) {
                        val pixelColor = Color(image.getRGB(x + i - 1, y + j - 1))
                        redSum += pixelColor.red * kernel[j][i]
                        greenSum += pixelColor.green * kernel[j][i]
                        blueSum += pixelColor.blue * kernel[j][i]
                    }
                }

                val red = redSum.coerceIn(0, 255)
                val green = greenSum.coerceIn(0, 255)
                val blue = blueSum.coerceIn(0, 255)

                result.setRGB(x, y, Color(red, green, blue).rgb)
            }
        }
        return result
    }
}
