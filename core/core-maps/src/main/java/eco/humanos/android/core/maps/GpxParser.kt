/**
 * core-maps / GpxParser.kt
 *
 * Parser GPX 1.1 mínimo y SEGURO (DOM javax.xml, disponible en JVM y Android). Lee:
 *  - <trk><trkseg><trkpt lat lon><ele/><time/> → una RouteGeometry por <trk>
 *  - <rte><rtept lat lon><ele/>               → una RouteGeometry por <rte>
 *  - <wpt lat lon><ele/><name/><type/>        → Waypoint
 *
 * Endurecido fail-closed: external entities / DTD desactivados (anti-XXE). Sin red.
 * Solo se usa con GPX fixture ficticio creado en el repo.
 */
package eco.humanos.android.core.maps

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

data class GpxDocument(val tracks: List<RouteGeometry>, val waypoints: List<Waypoint>)

class GpxParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

object GpxParser {

    fun parse(gpx: String): GpxDocument {
        val doc = try {
            secureFactory().newDocumentBuilder()
                .parse(ByteArrayInputStream(gpx.toByteArray(Charsets.UTF_8)))
        } catch (t: Throwable) {
            throw GpxParseException("GPX inválido: ${t.message}", t)
        }
        doc.documentElement.normalize()

        val tracks = ArrayList<RouteGeometry>()
        doc.byTag("trk").forEachIndexed { i, trk ->
            val pts = trk.descendants("trkpt").map { it.toGeoPoint() }
            if (pts.isNotEmpty()) tracks.add(RouteGeometry(id = "trk-$i", points = pts))
        }
        doc.byTag("rte").forEachIndexed { i, rte ->
            val pts = rte.descendants("rtept").map { it.toGeoPoint() }
            if (pts.isNotEmpty()) tracks.add(RouteGeometry(id = "rte-$i", points = pts))
        }
        val waypoints = doc.byTag("wpt").mapIndexed { i, wpt ->
            val g = wpt.toGeoPoint()
            Waypoint(
                id = "wpt-$i",
                name = wpt.childText("name") ?: "wpt-$i",
                lat = g.lat, lon = g.lon,
                category = wpt.childText("type"),
            )
        }
        return GpxDocument(tracks = tracks, waypoints = waypoints)
    }

    private fun secureFactory(): DocumentBuilderFactory =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            // anti-XXE: nada de DTD ni entidades externas
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isExpandEntityReferences = false
            isXIncludeAware = false
        }

    private fun org.w3c.dom.Document.byTag(tag: String): List<Element> =
        getElementsByTagName(tag).asElementList()

    private fun Element.descendants(tag: String): List<Element> =
        getElementsByTagName(tag).asElementList()

    private fun org.w3c.dom.NodeList.asElementList(): List<Element> =
        (0 until length).mapNotNull { item(it) as? Element }

    private fun Element.toGeoPoint(): GeoPoint {
        val lat = getAttribute("lat").toDoubleOrNull()
            ?: throw GpxParseException("punto sin lat")
        val lon = getAttribute("lon").toDoubleOrNull()
            ?: throw GpxParseException("punto sin lon")
        return GeoPoint(lat = lat, lon = lon, ele = childText("ele")?.toDoubleOrNull())
    }

    private fun Element.childText(tag: String): String? {
        val children = childNodes
        for (i in 0 until children.length) {
            val n = children.item(i)
            if (n.nodeType == Node.ELEMENT_NODE && (n as Element).tagName == tag) {
                return n.textContent?.trim()?.ifEmpty { null }
            }
        }
        return null
    }
}
