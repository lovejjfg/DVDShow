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

/**
 * Created by joe on 2018/12/22.
 * Email: lovejjfg@gmail.com
 */
class DVDView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private var startAngle = -(Math.PI / 6)
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

    init {
        paint.color = Color.RED
        paint.style = Paint.Style.FILL
        animator.duration = 3000
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            pathMeasure.getPosTan(value, floatArray, tanFloatArray)
//            println("角度：${tanFloatArray[0]}, ${tanFloatArray[1]}")
//            println("具体指：：：：：${floatArray[0]}, ${floatArray[1]}")
            tanValue = Math.abs(tanFloatArray[1] / tanFloatArray[0])
//            println("具体值：：：： $tanValue")
            val tanAngle = Math.atan(tanValue.toDouble()).toFloat()
//            println("计算角度：：：π/${Math.PI / tanAngle}")
            Log.e("calculate", "角度：tanValue:$tanValue")
            reCalculate = it.animatedFraction == 1f
            if (reCalculate) {
//                if (!needRevert) {
                startAngle += Math.PI / 2
//                }
                //
            }
            currentX = floatArray[0]
            currentY = floatArray[1]
            invalidate()

        }
        postDelayed({
            currentX = 0f
            currentY = rectF.height() * .2f

            calculate()
        }, 1000)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (changed) rectF.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        if (reCalculate) {
            calculate()
        }
        canvas.drawCircle(floatArray[0], floatArray[1], 10F, paint)
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
            handDown()
        } else if (startAngle > Math.PI / 2) {//(up)
            handUp()
        }
        pathMeasure.setPath(path, false)
        animator.setFloatValues(0F, pathMeasure.length)
        animator.start()
    }

    fun handUp() {
        if (currentX == 0f) {// normal
            val resultX = currentX + (currentY) / tanValue
            val dx = resultX - rectF.width()
            if (resultX > rectF.width()) {
                path.lineTo(rectF.width(), dx * tanValue)
            } else {
                path.lineTo(resultX, 0f)
            }
            needRevert = dx > 0
        } else if (currentX == rectF.width()) {// normal revert
            val resultX = (rectF.height() - currentY) / tanValue
            val dx = resultX - currentX
            if (currentX - resultX < 0) {
                path.lineTo(0f, rectF.height() - dx * tanValue)
            } else {
                path.lineTo(currentX - resultX, rectF.height())
            }
            needRevert = dx < 0
        } else if (needRevert) {
            val resultX = currentX
            val resultY = tanValue * resultX
            needRevert = if (resultY > rectF.height()) {
                val dy = resultY - rectF.height()
                val dx = dy / tanValue
                path.lineTo(dx, rectF.height())
                true
            } else {
                path.lineTo(0f, resultY)
                true
            }
        }
    }
    fun handRevertUp() {
        if (currentX == 0f) {// normal
            val resultX = currentX + (currentY) / tanValue
            val dx = resultX - rectF.width()
            if (resultX > rectF.width()) {
                path.lineTo(rectF.width(), dx * tanValue)
            } else {
                path.lineTo(resultX, 0f)
            }
            needRevert = dx > 0
        } else if (currentX == rectF.width()) {// normal revert
            val resultX = (rectF.height() - currentY) / tanValue
            val dx = resultX - currentX
            if (currentX - resultX < 0) {
                path.lineTo(0f, rectF.height() - dx * tanValue)
            } else {
                path.lineTo(currentX - resultX, rectF.height())
            }
            needRevert = dx < 0
        }
    }

    fun handDown() {
        val width = rectF.width()
        if (currentY == 0f) {//&& currentX != right// normal
            val resultY = (width - currentX) * tanValue
            val dy = resultY - rectF.height()
            needRevert = if (resultY > rectF.height()) {
                path.lineTo(width - dy / tanValue, rectF.height())
                true
            } else {
                path.lineTo(width, (width - currentX) * tanValue)
                false
            }
        } else if (currentY == rectF.height()) { // normal revert
            val resultY = currentY - (currentX) * tanValue
            val dy = -resultY
            needRevert = if (resultY < 0) {
                path.lineTo(dy / tanValue, currentY - (currentX) * tanValue)
                true
            } else {
                path.lineTo(0f, currentY - (currentX) * tanValue)
                false
            }
        } else if (needRevert) {
            val resultY = currentY
            val resultX = resultY / tanValue
            needRevert = if (resultX > rectF.width()) {//
                val dx = resultX - rectF.width()
                val dy = tanValue * dx
                path.lineTo(0f, dy)
                true
            } else {
                path.lineTo(currentX - resultX, 0f)
                true
            }
        }
    }
}
