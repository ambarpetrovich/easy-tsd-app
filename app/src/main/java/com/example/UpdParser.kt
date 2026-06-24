package com.example

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

data class UpdProduct(
    val lineNumber: String,
    val name: String,
    val quantity: String,
    val kizCodes: List<String>
)

object UpdParser {
    
    fun parseUpd(inputStream: InputStream): List<UpdProduct> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        val products = mutableListOf<UpdProduct>()
        var currentEvent = parser.eventType

        var currentProduct: UpdProduct? = null
        var kizCodes = mutableListOf<String>()
        var lineNumber = ""
        var name = ""
        var quantity = ""

        while (currentEvent != XmlPullParser.END_DOCUMENT) {
            when (currentEvent) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "СведТов" -> {
                            lineNumber = parser.getAttributeValue(null, "НомТек") ?: ""
                            name = parser.getAttributeValue(null, "НаимТов") ?: ""
                            quantity = parser.getAttributeValue(null, "КолТов") ?: ""
                            kizCodes = mutableListOf()
                        }
                        "КИЗ" -> {
                            if (parser.next() == XmlPullParser.TEXT) {
                                val kiz = parser.text.trim()
                                if (kiz.isNotEmpty()) {
                                    kizCodes.add(kiz)
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "СведТов") {
                        products.add(UpdProduct(lineNumber, name, quantity, kizCodes))
                        lineNumber = ""
                        name = ""
                        quantity = ""
                        kizCodes = mutableListOf()
                    }
                }
            }
            currentEvent = parser.next()
        }

        return products
    }
    
    fun extractAllKizCodes(inputStream: InputStream): List<String> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        val kizCodes = mutableListOf<String>()
        var currentEvent = parser.eventType

        while (currentEvent != XmlPullParser.END_DOCUMENT) {
            if (currentEvent == XmlPullParser.START_TAG && parser.name == "КИЗ") {
                if (parser.next() == XmlPullParser.TEXT) {
                    val kiz = parser.text.trim()
                    if (kiz.isNotEmpty()) {
                        kizCodes.add(kiz)
                    }
                }
            }
            currentEvent = parser.next()
        }

        return kizCodes
    }
}
