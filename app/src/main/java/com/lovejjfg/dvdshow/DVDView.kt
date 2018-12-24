package com.lovejjfg.dvdshow

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Created by joe on 2018/12/22.
 * Email: lovejjfg@gmail.com
 */
class DVDView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint: Paint = Paint(paint)
    private val rectF = RectF()
    private val ovalRectF = RectF()
    private var startAngle = (Math.PI * 2 / 3)
    private val startSide = 0
    private val path = Path()
    private val pathMeasure = PathMeasure()
    private val floatArray = FloatArray(2)
    private val tanFloatArray = FloatArray(8)
    private val animator = ValueAnimator()
    private var reCalculate = false
    private var currentX = 0f
    private var currentY = 0f
    private var tanValue = 1f
    private var needRevert = false
    private var maxLength = 0.toDouble()
    private val maxDuration: Long = 2000
    private var radius = 40f
    private var hintText = "DVD"
    private var textWidth = 0
    private var textHeight = 0
    private var state = STATE_DEFAULT
    private var shape = SHAPE_OVAL
    private val colors: IntArray = IntArray(4)
    private var calculateCount = 1

    init {
        colors[0] = Color.RED
        colors[1] = Color.BLUE
        colors[2] = Color.GREEN
        colors[3] = Color.YELLOW
        val a = context.obtainStyledAttributes(attrs, R.styleable.DVDView)
        radius = a.getDimension(R.styleable.DVDView_DVDRadio, context.dpToPx(20f))
        shape = a.getInt(R.styleable.DVDView_DVDShape, SHAPE_OVAL)
        a.recycle()
        paint.color = Color.RED
        paint.style = Paint.Style.FILL
        textPaint.textSize = context.spToPx(14f)
        textPaint.color = Color.WHITE
        val fontMetrics = Rect()
        textPaint.getTextBounds(hintText, 0, hintText.length, fontMetrics)
        textWidth = fontMetrics.width()
        textHeight = fontMetrics.height()
//        paint.co
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            pathMeasure.getPosTan(value, floatArray, tanFloatArray)
//            println("角度：${tanFloatArray[0]}, ${tanFloatArray[1]}")
//            println("具体指：：：：：${floatArray[0]}, ${floatArray[1]}")
            tanValue = Math.abs(tanFloatArray[1] / tanFloatArray[0])
//            println("具体值：：：： $tanValue")
//            println("计算角度：：：π/${Math.PI / tanAngle}")
            println(tanValue)
//            if (tanValue >= 1.74 || tanValue <= 1.72) {
//                throw IllegalStateException("参数不对了：$tanValue")
//            }
            if (tanValue.isNaN()) {
                throw IllegalStateException("参数不对了：y:${tanFloatArray[1]}  x:${tanFloatArray[0]}")
            }
//            Log.e("calculate", "角度：tanValue:$tanValue")
            reCalculate = it.animatedFraction == 1f
            if (reCalculate) {
                ++calculateCount
                paint.color = colors[calculateCount % colors.size]
                startAngle += Math.PI / 2
            }
            currentX = floatArray[0]
            currentY = floatArray[1]
            ovalRectF.set(
                currentX - 1.5f * radius, currentY - radius, currentX + 1.5f * radius,
                currentY + radius
            )
            invalidate()

        }
        postDelayed({
            currentX = ovalX()
            currentY = rectF.height() * .4f
            tanValue = Math.abs(Math.tan(startAngle).toFloat())
            maxLength = Math.sqrt(rectF.width() * rectF.width().toDouble() + rectF.height() * rectF.height().toDouble())

            calculate()
        }, 1000)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (changed) rectF.set(
            left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat()
        )
    }

    override fun onDraw(canvas: Canvas) {
        handDraw(canvas)
    }

    private fun handDraw(canvas: Canvas) {
        if (floatArray[0] == 0f && floatArray[1] == 0f) {
            return
        }
        if (reCalculate) {
            calculate()
        }
        if (shape == SHAPE_OVAL) {
            canvas.drawOval(ovalRectF, paint)
        } else {
            canvas.drawCircle(floatArray[0], floatArray[1], radius, paint)
        }
        canvas.drawText(
            hintText,
            ovalRectF.centerX() - textWidth * .5f,
            ovalRectF.centerY() + textHeight * .5f,
            textPaint
        )
    }

    private fun calculate() {
        //(+,-)(+,+)(-,+)(-,-)
        //(-PI/4 PI/4 3PI/4 5PI/4) (PI/4(down) 3PI/4(up))
        if (state == STATE_ERROR) {
            return
        }
        when {
            startAngle == 0.toDouble() -> startAngle = 3 * Math.PI / 4
            startAngle < 0 -> while (startAngle < 0) {
                startAngle += Math.PI
            }
            else -> while (startAngle > Math.PI) {
                startAngle -= Math.PI
            }
        }
        path.reset()
        path.moveTo(currentX, currentY)
        if (startAngle < Math.PI / 2) {//(down)
            if (needRevert) {
                handRecertDown()
            } else {
                handDown()
            }
        } else if (startAngle > Math.PI / 2) {//(up)
            if (needRevert) {
                handRevertUp()
            } else {
                handUp()
            }
        }
        pathMeasure.setPath(path, false)

        val length = pathMeasure.length
        animator.duration = (length / maxLength * maxDuration).toLong()
        animator.setFloatValues(0F, length)
        animator.start()
    }

    private fun ovalX() = if (shape == SHAPE_CIRCLE) radius else radius * 1.382f
    private fun ovalY() = radius
    private fun startX() = ovalX()
    private fun endX() = rectF.width() - ovalX()
    private fun startY() = ovalY()
    private fun endY() = rectF.height() - ovalY()

    private fun handUp() {
        if (currentX == startX()) {// normal
            val resultX = startX() + (currentY - startY()) / tanValue
            val dx = resultX - endX()
            if (resultX > endX()) {
                Log.e("calculate", "上升 handUp:越界情况")
                val dy = tanValue * (endX() - currentX)
                path.lineTo(endX(), currentY - dy)
            } else {
                Log.e("calculate", "上升 handUp:正常情况")
                path.lineTo(resultX, startY())
            }
            needRevert = dx > 0
        } else if (currentX == endX()) {// normal revert
            val resultX = (endY() - currentY) / tanValue
            val dx = resultX - currentX
            if (currentX - resultX < startX()) {
                Log.e("calculate", " handUp 下降:越界情况")
                path.lineTo(startX(), rectF.height() - dx * tanValue)
                needRevert = true
            } else {
                Log.e("calculate", " handUp 下降:正常情况")
                path.lineTo(currentX - resultX, rectF.height() - ovalY())
                needRevert = false
            }
        } else {
            Log.e(
                "calculate",
                " handUp 异常情况：currentX =$currentX startX=${startX()} endx:${endX()} startX=${startX()} endx:${endX()}"
            )
            animator.cancel()
            state = STATE_ERROR
        }
    }

    private fun handRevertUp() {
        if (currentY == startY()) {// normal
            val resultX = currentX - startX()
            val resultY = tanValue * resultX + startY()
            if (resultY > endY()) {
                val dx = (endY() - currentY) / tanValue
                Log.e("calculate", "下降 handRevertUp:越界情况")
                path.lineTo(currentX - dx, endY())
                if (currentX - dx < startX()) {
                    throw RuntimeException("${currentX - dx} 不符合规范")
                }
                needRevert = false
            } else {
                Log.e("calculate", "下降 handRevertUp:正常情况")
                path.lineTo(startX(), resultY)
                needRevert = true
            }
        } else if (currentY == endY()) {// normal revert
            var resultX = endX()
            val dx = resultX - currentX
            val resultY = currentY - dx * tanValue
            if (resultY < startY()) {
                val ddy = currentY - startY()
                val ddx = ddy / tanValue
                Log.e("calculate", " handRevertUp 上升:越界情况")
                resultX = currentX + ddx
                path.lineTo(resultX, startY())
                needRevert = false
            } else {
                //fix
                Log.e("calculate", " handRevertUp 上升:正常情况")
                path.lineTo(endX(), resultY)
                needRevert = true
            }
        } else {
            Log.e("calculate", " handRevertUp 异常情况：currentY =$currentY startY=${startY()} endY:${endY()} ")
            animator.cancel()
            state = STATE_ERROR
        }
    }

    private fun handDown() {
        if (currentY == startY()) {//&& currentX != right// normal
            val resultY = (endX() - currentX) * tanValue + startY()
            needRevert = if (resultY > endY()) {
                path.lineTo(currentX + (endY() - startY()) / tanValue, endY())
                Log.e("calculate", " handDown 下降:越界情况")
                true
            } else {
                path.lineTo(endX(), resultY)
                Log.e("calculate", " handDown 下降:正常情况")
                false
            }
        } else if (currentY == endY()) { // normal revert
            val resultY = currentY - (currentX - startX()) * tanValue
            val dy = currentY - startY()
            needRevert = if (resultY < startY()) {
                path.lineTo(currentX - dy / tanValue, startY())
                Log.e("calculate", "上升 handDown:越界情况")
                true
            } else {
                path.lineTo(startX(), currentY - (currentX - startX()) * tanValue)
                Log.e("calculate", "上升 handDown:正常情况")
                false
            }
        } else {
            Log.e("calculate", " handDown 异常情况：currentY =$currentY startY=${startY()} endY:${endY()}")
            animator.cancel()
            state = STATE_ERROR
        }
    }

    private fun handRecertDown() {
        if (currentX == endX()) {//&& currentX != right// normal
            val resultY = startY()
            val resultX = (currentY - resultY) / tanValue
            if (resultX > rectF.width() - ovalX()) {
                //fix
                val dx = currentX - startX()
                val dy = dx * tanValue
                path.lineTo(startX(), dy)
                Log.e("calculate", "上升 handRecertDown:越界情况")
                needRevert = false
            } else {
                //fix
                path.lineTo(currentX - resultX, startY())
                Log.e("calculate", "上升 handRecertDown:正常情况")
                needRevert = true
            }
        } else if (currentX == startX()) { // normal revert
            val dy = endY() - currentY
            val resultX = dy / tanValue + startX()
            if (resultX > endX()) {
                val dx = endX() - currentX
                val resultY = currentY + dx * tanValue
                path.lineTo(endX(), resultY)
                Log.e("calculate", " handRecertDown 下降:越界情况")
                needRevert = false
            } else {
                path.lineTo(resultX, endY())
                Log.e("calculate", " handRecertDown 下降:正常情况")
                needRevert = true
            }
        } else {
            Log.e("calculate", " handRecertDown 异常情况：currentX =$currentX startX=${startX()} endx:${endX()}")
            animator.cancel()
            state = STATE_ERROR
        }
    }

    private fun Context.dpToPx(dp: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    private fun Context.spToPx(sp: Float): Float {
        val scale = resources.displayMetrics.scaledDensity
        return (sp * scale + 0.5f)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
        state = STATE_ERROR

    }

    companion object {
        const val STATE_DEFAULT = 1
        //todo handle success
        const val STATE_SUCCESS = 2
        const val STATE_ERROR = 3
        const val SHAPE_OVAL = 1
        const val SHAPE_CIRCLE = 0
    }
}
