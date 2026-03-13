package com.stickersnip.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Custom view that displays an image with a draggable crop rectangle.
 * Features 8 handles (corners + midpoints) and rule-of-thirds grid lines.
 */
class CropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null

    // Crop rect in image coordinates
    private var cropRect = RectF()

    // Display transform: image → view coordinates
    private var offsetX = 0f
    private var offsetY = 0f
    private var scale = 1f

    // Handle size in pixels
    private val handleRadius = 20f
    private val handleTouchRadius = 40f

    // Currently active handle (-1 = none, 0-7 = handles, 8 = drag entire rect)
    private var activeHandle = -1
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFE135")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFE135")
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#55FFE135")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#88000000")
        style = Paint.Style.FILL
    }

    /**
     * Sets the source bitmap and applies auto-crop detection.
     */
    fun setImageBitmap(bmp: Bitmap) {
        bitmap = bmp
        val detected = EdgeDetector.detectEdges(bmp)
        cropRect = RectF(
            detected.left.toFloat(),
            detected.top.toFloat(),
            detected.right.toFloat(),
            detected.bottom.toFloat()
        )
        requestLayout()
        invalidate()
    }

    /**
     * Returns the cropped bitmap based on the current crop rectangle.
     */
    fun getCroppedBitmap(): Bitmap? {
        val bmp = bitmap ?: return null
        val left = cropRect.left.toInt().coerceIn(0, bmp.width - 1)
        val top = cropRect.top.toInt().coerceIn(0, bmp.height - 1)
        val right = cropRect.right.toInt().coerceIn(left + 1, bmp.width)
        val bottom = cropRect.bottom.toInt().coerceIn(top + 1, bmp.height)
        return Bitmap.createBitmap(bmp, left, top, right - left, bottom - top)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeTransform()
    }

    private fun computeTransform() {
        val bmp = bitmap ?: return
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        if (viewW <= 0 || viewH <= 0) return

        val scaleX = viewW / bmp.width
        val scaleY = viewH / bmp.height
        scale = min(scaleX, scaleY)
        offsetX = (viewW - bmp.width * scale) / 2f
        offsetY = (viewH - bmp.height * scale) / 2f
    }

    // Convert image coords to view coords
    private fun toViewX(imgX: Float) = imgX * scale + offsetX
    private fun toViewY(imgY: Float) = imgY * scale + offsetY

    // Convert view coords to image coords
    private fun toImgX(viewX: Float) = (viewX - offsetX) / scale
    private fun toImgY(viewY: Float) = (viewY - offsetY) / scale

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return
        computeTransform()

        // Draw the image
        val dstRect = RectF(
            offsetX, offsetY,
            offsetX + bmp.width * scale,
            offsetY + bmp.height * scale
        )
        canvas.drawBitmap(bmp, null, dstRect, null)

        // Draw semi-transparent overlay outside crop area
        val vl = toViewX(cropRect.left)
        val vt = toViewY(cropRect.top)
        val vr = toViewX(cropRect.right)
        val vb = toViewY(cropRect.bottom)

        // Top overlay
        canvas.drawRect(dstRect.left, dstRect.top, dstRect.right, vt, overlayPaint)
        // Bottom overlay
        canvas.drawRect(dstRect.left, vb, dstRect.right, dstRect.bottom, overlayPaint)
        // Left overlay
        canvas.drawRect(dstRect.left, vt, vl, vb, overlayPaint)
        // Right overlay
        canvas.drawRect(vr, vt, dstRect.right, vb, overlayPaint)

        // Draw crop border
        canvas.drawRect(vl, vt, vr, vb, borderPaint)

        // Draw rule-of-thirds grid lines
        val thirdW = (vr - vl) / 3f
        val thirdH = (vb - vt) / 3f
        canvas.drawLine(vl + thirdW, vt, vl + thirdW, vb, gridPaint)
        canvas.drawLine(vl + 2 * thirdW, vt, vl + 2 * thirdW, vb, gridPaint)
        canvas.drawLine(vl, vt + thirdH, vr, vt + thirdH, gridPaint)
        canvas.drawLine(vl, vt + 2 * thirdH, vr, vt + 2 * thirdH, gridPaint)

        // Draw 8 handles
        val handles = getHandlePositions(vl, vt, vr, vb)
        for (pos in handles) {
            canvas.drawCircle(pos[0], pos[1], handleRadius, handlePaint)
        }
    }

    private fun getHandlePositions(l: Float, t: Float, r: Float, b: Float): Array<FloatArray> {
        val cx = (l + r) / 2f
        val cy = (t + b) / 2f
        return arrayOf(
            floatArrayOf(l, t),       // 0: top-left
            floatArrayOf(cx, t),      // 1: top-center
            floatArrayOf(r, t),       // 2: top-right
            floatArrayOf(r, cy),      // 3: right-center
            floatArrayOf(r, b),       // 4: bottom-right
            floatArrayOf(cx, b),      // 5: bottom-center
            floatArrayOf(l, b),       // 6: bottom-left
            floatArrayOf(l, cy)       // 7: left-center
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bitmap == null) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                val vl = toViewX(cropRect.left)
                val vt = toViewY(cropRect.top)
                val vr = toViewX(cropRect.right)
                val vb = toViewY(cropRect.bottom)

                val handles = getHandlePositions(vl, vt, vr, vb)

                // Check if touching a handle
                activeHandle = -1
                for (i in handles.indices) {
                    if (abs(x - handles[i][0]) < handleTouchRadius &&
                        abs(y - handles[i][1]) < handleTouchRadius
                    ) {
                        activeHandle = i
                        break
                    }
                }

                // Check if touching inside rect (drag)
                if (activeHandle == -1 && x in vl..vr && y in vt..vb) {
                    activeHandle = 8
                }

                lastTouchX = event.x
                lastTouchY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (activeHandle < 0) return false
                val bmp = bitmap ?: return false

                val dx = toImgX(event.x) - toImgX(lastTouchX)
                val dy = toImgY(event.y) - toImgY(lastTouchY)

                val minSize = 50f
                val maxW = bmp.width.toFloat()
                val maxH = bmp.height.toFloat()

                when (activeHandle) {
                    0 -> { // top-left
                        cropRect.left = (cropRect.left + dx).coerceIn(0f, cropRect.right - minSize)
                        cropRect.top = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - minSize)
                    }
                    1 -> { // top-center
                        cropRect.top = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - minSize)
                    }
                    2 -> { // top-right
                        cropRect.right = (cropRect.right + dx).coerceIn(cropRect.left + minSize, maxW)
                        cropRect.top = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - minSize)
                    }
                    3 -> { // right-center
                        cropRect.right = (cropRect.right + dx).coerceIn(cropRect.left + minSize, maxW)
                    }
                    4 -> { // bottom-right
                        cropRect.right = (cropRect.right + dx).coerceIn(cropRect.left + minSize, maxW)
                        cropRect.bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, maxH)
                    }
                    5 -> { // bottom-center
                        cropRect.bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, maxH)
                    }
                    6 -> { // bottom-left
                        cropRect.left = (cropRect.left + dx).coerceIn(0f, cropRect.right - minSize)
                        cropRect.bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, maxH)
                    }
                    7 -> { // left-center
                        cropRect.left = (cropRect.left + dx).coerceIn(0f, cropRect.right - minSize)
                    }
                    8 -> { // drag entire rect
                        val w = cropRect.width()
                        val h = cropRect.height()
                        var newLeft = cropRect.left + dx
                        var newTop = cropRect.top + dy
                        newLeft = newLeft.coerceIn(0f, maxW - w)
                        newTop = newTop.coerceIn(0f, maxH - h)
                        cropRect.set(newLeft, newTop, newLeft + w, newTop + h)
                    }
                }

                lastTouchX = event.x
                lastTouchY = event.y
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = -1
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
