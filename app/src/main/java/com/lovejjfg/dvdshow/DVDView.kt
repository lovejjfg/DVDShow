package com.lovejjfg.dvdshow

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
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
    private val rectF = RectF()
    private var startAngle = (Math.PI * 3 / 4)
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
    private var radius = 20f

    init {
        paint.color = Color.RED
        paint.style = Paint.Style.FILL
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            pathMeasure.getPosTan(value, floatArray, tanFloatArray)
//            println("角度：${tanFloatArray[0]}, ${tanFloatArray[1]}")
//            println("具体指：：：：：${floatArray[0]}, ${floatArray[1]}")
            tanValue = Math.abs(tanFloatArray[1] / tanFloatArray[0])
//            println("具体值：：：： $tanValue")
//            println("计算角度：：：π/${Math.PI / tanAngle}")
            if (tanValue > 1.74) {
                throw IllegalStateException("参数不对了：$tanValue")
            }
            if (tanValue.isNaN()) {
                throw IllegalStateException("参数不对了：y:${tanFloatArray[1]}  x:${tanFloatArray[0]}")
            }
//            Log.e("calculate", "角度：tanValue:$tanValue")
            reCalculate = it.animatedFraction == 1f
            if (reCalculate) {
                startAngle += Math.PI / 2
            }
            currentX = floatArray[0]
            currentY = floatArray[1]
            invalidate()

        }
        postDelayed({
            currentX = 0f
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
        if (reCalculate) {
            calculate()
        }
        canvas.drawCircle(floatArray[0], floatArray[1], radius, paint)
    }

    private fun calculate() {
        //(+,-)(+,+)(-,+)(-,-)
        //(-PI/4 PI/4 3PI/4 5PI/4) (PI/4(down) 3PI/4(up))
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

    fun handUp() {
        if (currentX == 0f) {// normal
            val resultX = currentX + (currentY) / tanValue
            val dx = resultX - rectF.width()
            if (resultX > rectF.width()) {
                Log.e("calculate", "上升 handUp:越界情况")
                path.lineTo(rectF.width(), dx * tanValue)
            } else {
                Log.e("calculate", "上升 handUp:正常情况")
                path.lineTo(resultX, 0f)
            }
            needRevert = dx > 0
        } else if (currentX == rectF.width()) {// normal revert
            val resultX = (rectF.height() - currentY) / tanValue
            val dx = resultX - currentX
            if (currentX - resultX < 0) {
                Log.e("calculate", " handUp 下降:越界情况")
                path.lineTo(0f, rectF.height() - dx * tanValue)
                needRevert = true
            } else {
                Log.e("calculate", " handUp 下降:正常情况")
                path.lineTo(currentX - resultX, rectF.height())
                needRevert = false
            }
        }
    }

    fun handRevertUp() {
        if (currentY == 0f) {// normal
            val resultX = currentX
            val resultY = tanValue * resultX
            val dy = resultY - rectF.height()
            val dx = dy / tanValue
            if (dy > 0) {
                Log.e("calculate", "下降 handRevertUp:越界情况")
                path.lineTo(dx, rectF.height())
                needRevert = false
            } else {
                Log.e("calculate", "下降 handRevertUp:正常情况")
                path.lineTo(0f, resultY)
                needRevert = true
            }
        } else if (currentY == rectF.height()) {// normal revert
            //todo
            var resultX = rectF.width()
            val dx = resultX - currentX
            val resultY = dx * tanValue
            val dy = resultY - rectF.height()
            if (dy > 0) {
                Log.e("calculate", " handRevertUp 上升:越界情况")
                resultX = rectF.height() / tanValue
                path.lineTo(currentX + resultX, 0f)
                needRevert = false
            } else {
                Log.e("calculate", " handRevertUp 上升:正常情况")
                path.lineTo(rectF.width(), -dy)
                needRevert = true
            }
        }
    }

    fun handDown() {
        val width = rectF.width()
        if (currentY == 0f) {//&& currentX != right// normal
            val resultY = (width - currentX) * tanValue
            val dy = resultY - rectF.height()
            needRevert = if (resultY > rectF.height()) {
                path.lineTo(width - dy / tanValue, rectF.height())
                Log.e("calculate", " handDown 下降:越界情况")
                true
            } else {
                path.lineTo(width, (width - currentX) * tanValue)
                Log.e("calculate", " handDown 下降:正常情况")
                false
            }
        } else if (currentY == rectF.height()) { // normal revert
            val resultY = currentY - (currentX) * tanValue
            val dy = -resultY
            needRevert = if (resultY < 0) {
                path.lineTo(dy / tanValue, 0f)
                Log.e("calculate", "上升 handDown:越界情况")
                true
            } else {
                path.lineTo(0f, currentY - (currentX) * tanValue)
                Log.e("calculate", "上升 handDown:正常情况")
                false
            }
        }
    }

    fun handRecertDown() {
        if (currentX == rectF.width()) {//&& currentX != right// normal
            val resultY = 0f
            val resultX = (currentY - resultY) / tanValue
            if (resultX > rectF.width()) {
                val dx = resultX - rectF.width()
                val dy = dx * tanValue
                path.lineTo(0f, dy)
                Log.e("calculate", "上升 handRecertDown:越界情况")
                needRevert = false
            } else {
                path.lineTo(currentX - resultX, resultY)
                Log.e("calculate", "上升 handRecertDown:正常情况")
                needRevert = true
            }
        } else if (currentX == 0f) { // normal revert
            val resultY = rectF.height()
            val dy = resultY - currentY
            val resultX = dy / tanValue
            if (resultX > rectF.width()) {
                val dx = resultX - rectF.width()
                path.lineTo(rectF.width(), resultY - dx * tanValue)
                Log.e("calculate", " handRecertDown 下降:越界情况")
                needRevert = false
            } else {
                path.lineTo(resultX, resultY)
                Log.e("calculate", " handRecertDown 下降:正常情况")
                needRevert = true
            }
        }
    }
}
