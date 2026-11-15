package com.tkasmp2027.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class RichHtmlCachedImageTest {

    @Test
    fun mapsExactOfficialImagesToVerifiedLocalAssets() {
        val expectedAsset = "cbt_images/reference_renovation_building.png"
        val renovationUrls = listOf(
            "https://pusmendik.kemendikdasmen.go.id/tka/cbt_images/95316_dae269e04f8b0f9af69d3e8749c3f29b.png",
            "https://pusmendik.kemendikdasmen.go.id/tka/cbt_images/90400_dae269e04f8b0f9af69d3e8749c3f29b.png",
            "https://pusmendik.kemendikdasmen.go.id/tka/cbt_images/30999_dae269e04f8b0f9af69d3e8749c3f29b.png"
        )

        renovationUrls.forEach { url ->
            assertEquals(expectedAsset, cachedOfficialImageAssetForUrl(url))
        }

        val packageTwoImages = listOf(
            "56579_f243848bfdf25d16000c8d54e9a10b6c.jpg" to "9AD01BA1EEC3274ECB5C8FC1741D4C75BC5010FE9087A82F65E66D0F7C1EE25D",
            "56579_2acd540dddff3758f83e84fc4cf37932.jpg" to "BDC8366867792D6E00889CE90783DD61AFD7C80A164B8D6E20881335600AB70E",
            "56579_0319187eee890ee75d6bed18baad59f4.jpg" to "2E62101399EB149F06AEAD840BDF1954DE8288E4CA63087C9A46C6099FB55AAB",
            "56579_f70e4743bb1152db90898e2a5d9f3f60.jpg" to "0D551ECBD47F05D532940D2C683526054889BE9D19C364D8A086CFB271E9173B",
            "56579_478f9cb2311328eee0271ccf697beb9b.jpg" to "B63DE287867B19E05DBFCA2D1C803806FCCBB2AED9A98B9E086E1A0838398DCD",
            "45215_ad80fefdd28886cfac6e144f2d04fc62.jpg" to "726F284B59CF2F5EC80BA2AC3512E4F78ECF94AA5CCBAED3FE63A54C4B017488",
            "74495_5dd13f231102ae213e1e415778941ef1.jpg" to "8196B9098454C867E46907C875F10FBECEA55D9F0E5B32C2AE62A6A7AE7E5D98",
            "59990_5e2e8f5da5c7000b28ea76db33139345.jpg" to "E65058A7E44BAE6DB9F7B35A6EF3A69F2A1399D06460DBA6CB1EB1CDC1B63CB9",
            "59990_93616fe93e4b426cd4616f180ec749bd.jpg" to "CC5756FB5FCCC26AB3B5AE35792FE5F04635DF2F97C42D4E2149244AABF54C35",
            "59990_9562b3cc10f2a24941a7d51140570711.jpg" to "0E1F1F7CB3AE23A077F815F34C038E453CEA4EC36DE23759CD00C5520376973C",
            "59990_f307ed6cbe35344643e3249a8ab7cfb7.jpg" to "4AF2AD5CB558608829D2EB56F0CC7D0856C093BF22ADEDD33B059C10ADAF6F40",
            "59990_3a0cb35210de3fc69253eecefb4f5f43.jpg" to "780A68E028A96E611DDBF8FE59B7CE9ACDF0A271385351CC791BD0C4B964EE0A"
        )
        packageTwoImages.forEach { (name, expectedSha256) ->
            val url = "https://pusmendik.kemendikdasmen.go.id/tka/cbt_images/$name"
            val assetPath = "cbt_images/matematika_paket_2/$name"
            assertEquals(assetPath, cachedOfficialImageAssetForUrl(url))

            val cacheFile = File("src/main/assets/$assetPath")
            assertTrue("Verified image cache asset is missing: $assetPath", cacheFile.isFile)
            val sha256 = MessageDigest.getInstance("SHA-256")
                .digest(cacheFile.readBytes())
                .joinToString("") { byte -> "%02X".format(byte.toInt() and 0xFF) }
            assertEquals("Unexpected official image bytes for $name", expectedSha256, sha256)
        }

        assertNull(
            cachedOfficialImageAssetForUrl(
                "https://example.com/tka/cbt_images/95316_dae269e04f8b0f9af69d3e8749c3f29b.png"
            )
        )
        assertNull(
            cachedOfficialImageAssetForUrl(
                "https://pusmendik.kemendikdasmen.go.id/tka/cbt_images/unrelated.png"
            )
        )
        assertNull(
            cachedOfficialImageAssetForUrl(
                "http://pusmendik.kemendikdasmen.go.id/tka/cbt_images/56579_f243848bfdf25d16000c8d54e9a10b6c.jpg"
            )
        )

        val cacheFile = File("src/main/assets/$expectedAsset")
        assertTrue("Verified image cache asset is missing", cacheFile.isFile)
        assertEquals(668_646L, cacheFile.length())
        val sha256 = MessageDigest.getInstance("SHA-256")
            .digest(cacheFile.readBytes())
            .joinToString("") { byte -> "%02X".format(byte.toInt() and 0xFF) }
        assertEquals(
            "01430213BCC33C2B034556672587AD3365485A492D9DD3499709137E4F896DC0",
            sha256
        )
    }

    @Test
    fun formatsFormulaAltTextAsReadableMathematics() {
        assertEquals(
            "Rumus: (7³ × 7⁻⁴) / 7²",
            readableImageAltFallback("\\frac{(7^{3}\\times 7^{-4})}{7^{2}}")
        )
        assertEquals(
            "Rumus: 2ab - b² + 3a²b + ab² - 5",
            readableImageAltFallback("2ab-b^{2}+3a^{2}b+ab^{2}-5")
        )
        assertEquals(
            "Rumus: 3x + 17 ≤ 7 - 2x",
            readableImageAltFallback("3x+17\\leq 7-2x")
        )
        assertEquals(
            "Diagram atau gambar soal tidak dapat dimuat.",
            readableImageAltFallback("")
        )
    }
}
