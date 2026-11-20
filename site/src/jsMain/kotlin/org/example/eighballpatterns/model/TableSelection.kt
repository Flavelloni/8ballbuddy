package org.example.eighballpatterns.model

internal fun quarterLabel(quarter: Int?): String = when (quarter) {
    1 -> "top-left"
    2 -> "top-right"
    3 -> "bottom-left"
    4 -> "bottom-right"
    else -> "unknown"
}

/** Clockwise corners of the cloth in the final photograph, starting at top-left. */
internal data class TableSelection(
    val corners: List<ImagePoint>, val imageWidth: Int, val imageHeight: Int, val imageSha256: String,
) {
    /** Project the unit table onto the photograph, including perspective foreshortening. */
    fun point(u: Double, v: Double): ImagePoint {
        val (a,b,c,d)=corners
        val dx1=b.x-c.x; val dx2=d.x-c.x; val dx3=a.x-b.x+c.x-d.x
        val dy1=b.y-c.y; val dy2=d.y-c.y; val dy3=a.y-b.y+c.y-d.y
        val determinant=dx1*dy2-dx2*dy1
        val g=(dx3*dy2-dx2*dy3)/determinant
        val h=(dx1*dy3-dx3*dy1)/determinant
        val scale=g*u+h*v+1
        return ImagePoint(((b.x-a.x+g*b.x)*u+(d.x-a.x+h*d.x)*v+a.x)/scale,
            ((b.y-a.y+g*b.y)*u+(d.y-a.y+h*d.y)*v+a.y)/scale)
    }
    val quarters: List<List<ImagePoint>> get() {
        val (a,b,c,d) = corners
        val top=point(.5,0.0); val right=point(1.0,.5); val bottom=point(.5,1.0); val left=point(0.0,.5)
        val center=point(.5,.5)
        return listOf(listOf(a,top,center,left), listOf(top,b,right,center),
            listOf(left,center,bottom,d), listOf(center,right,c,bottom))
    }
    fun quarterAt(point: ImagePoint): Int? {
        if (!point.x.isFinite() || !point.y.isFinite()) return null
        return quarters.indexOfFirst { polygon -> polygon.indices.all { i ->
            val a=polygon[i]; val b=polygon[(i+1)%4]
            (b.x-a.x)*(point.y-a.y)-(b.y-a.y)*(point.x-a.x) >= -1e-9
        } }.takeIf { it >= 0 }?.plus(1)
    }
    fun center(quarter: Int): ImagePoint {
        return point(if (quarter in listOf(1,3)) .25 else .75, if (quarter in 1..2) .25 else .75)
    }
}
