package ru.bedsus.pincodecustomview

import android.content.Context
import android.graphics.*
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import ru.bedsus.pincodecustomview.PinView.Companion.DEFAULT_PIN_LENGTH
import kotlin.math.abs

/**
 * Поле ввода кода подтверждения
 * Имеет параметр числа символов в поле [DEFAULT_PIN_LENGTH]
 */
class PinView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var sourceText: String = ""
    private var sourceTextLength: Int = 0

    /**
     * Длина пин-кода.
     * Ограничена от 3 до 8 элементов.
     */
    private var pinLength = DEFAULT_PIN_LENGTH
        set(value) {
            field = when {
                value < MIN_PIN_LENGTH -> MIN_PIN_LENGTH
                value > MAX_PIN_LENGTH -> MAX_PIN_LENGTH
                else -> value
            }
            setMaxLength(field)
            requestLayout()
        }

    private var itemWidth: Float
    private var itemHeight: Float
    private var itemCornerRadius: Float
    private var itemBorderWidth: Float
    private var selectedItemBorderWidth: Float
    private var itemSpacing: Float

    @ColorInt
    var itemBorderColor = ContextCompat.getColor(context, R.color.border)
        set(value) {
            field = value
            updateItemPaint()
            invalidate()
        }

    /**
     * Колбэк, вызывается когда кол-во введеных символов равно длине пин-кода.
     */
    var onPinFullListener: (String) -> Unit = {}

    private var itemRectF = RectF()
    private var textRect = Rect()
    private val itemCenterPointF = PointF()
    private var itemPaint = Paint(paint)

    init {
        setBackgroundResource(0)

        /**
         * Переменная для перевода dp в px.
         * Вся отрисовка производится в px.
         */
        val density = context.resources.displayMetrics.density

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PinView,
            defStyleAttr,
            0
        ).apply {
            try {
                pinLength = getInteger(R.styleable.PinView_itemCount, DEFAULT_PIN_LENGTH)

                itemWidth = getDimension(
                    R.styleable.PinView_itemWidth,
                    DEFAULT_ITEM_WIDTH
                ) * density

                itemHeight = getDimension(
                    R.styleable.PinView_itemHeight,
                    DEFAULT_ITEM_HEIGHT
                ) * density

                itemCornerRadius = getDimension(
                    R.styleable.PinView_itemCornerRadius,
                    DEFAULT_ITEM_CORNER_RADIUS
                ) * density

                itemBorderWidth = getFloat(
                    R.styleable.PinView_itemBorderWidth,
                    DEFAULT_ITEM_BORDER_WIDTH
                ) * density

                selectedItemBorderWidth = getFloat(
                    R.styleable.PinView_selectedItemBorderWidth,
                    DEFAULT_SELECTED_ITEM_BORDER_WIDTH
                ) * density

                itemSpacing = getDimension(
                    R.styleable.PinView_itemSpacing,
                    DEFAULT_ITEM_SPACING
                ) * density

                itemBorderColor = getColor(
                    R.styleable.PinView_itemBorderColor,
                    ContextCompat.getColor(context, R.color.border)
                )
            } finally {
                recycle()
            }
        }
        setMaxLength(pinLength)
        updateItemPaint()
        disableSelectionMenu()
        super.setCursorVisible(false)
        super.setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL)
    }

    /**
     * Устанавливает фильтр на длину вводимого текста.
     * Текст не может превышать длину пин-кода.
     */
    private fun setMaxLength(maxLength: Int) {
        filters = arrayOf(InputFilter.LengthFilter(maxLength))
    }

    private fun updateItemPaint() {
        itemPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = itemBorderWidth
            color = itemBorderColor
        }
    }

    override fun onTextChanged(
        text: CharSequence?,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        sourceText = text.toString()
        sourceTextLength = sourceText.length
        if (sourceTextLength == pinLength && pinLength != 0) {
            onPinFullListener(sourceText)
        }
        moveSelectionToEnd()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        var width = widthSize
        var height = heightSize

        if (widthMode != MeasureSpec.EXACTLY) {
            width = (itemWidth * pinLength + (itemSpacing * (pinLength - 1))).toInt() +
                    ViewCompat.getPaddingEnd(this) + ViewCompat.getPaddingStart(this)
        }

        if (heightMode != MeasureSpec.EXACTLY) {
            height = (itemHeight + paddingTop + paddingBottom).toInt()
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        repeat(pinLength) { itemPosition ->
            highlightItem(itemPosition)

            if (itemPosition == 1) {
                canvas.translate(itemSpacing + itemWidth + paddingStart, 0f)
            } else if (itemPosition > 1) {
                canvas.translate(itemSpacing + itemWidth, 0f)
            }

            updateItemRect(itemPosition)

            canvas.drawRoundRect(
                itemRectF,
                itemCornerRadius,
                itemCornerRadius,
                itemPaint
            )
            updateCenterPoint()

            if (sourceText.length > itemPosition) {
                paint.getTextBounds(sourceText, itemPosition, itemPosition + 1, textRect)
                val x = itemCenterPointF.x - abs(textRect.width()) / 2 - textRect.left
                val y = itemCenterPointF.y + abs(textRect.height()) / 2 - textRect.bottom
                canvas.drawText(sourceText, itemPosition, itemPosition + 1, x, y, paint)
            }
        }
    }

    /**
     * Подсвечивает элемент, в который будет печататься следующий символ.
     * Выполняет роль визуального фокуса.
     */
    private fun highlightItem(itemPosition: Int) {
        val isHighlight = isFocused && itemPosition == sourceTextLength
        itemPaint.strokeWidth =
            if (isHighlight) selectedItemBorderWidth
            else itemBorderWidth
    }

    private fun updateItemRect(itemPosition: Int) {
        if (itemPosition == 0) {
            itemRectF.set(
                paddingStart.toFloat(),
                paddingTop.toFloat(),
                itemWidth + paddingStart,
                itemHeight + paddingTop
            )
        } else {
            itemRectF.set(
                0f,
                paddingTop.toFloat(),
                itemWidth,
                itemHeight + paddingTop
            )
        }
    }

    private fun updateCenterPoint() {
        val centerX = itemRectF.left + abs(itemRectF.width()) / 2
        val centerY = itemRectF.top + abs(itemRectF.height()) / 2
        itemCenterPointF.set(centerX, centerY)
    }

    private fun moveSelectionToEnd() {
        setSelection(sourceTextLength)
    }

    /**
     * Отключает всплываюшее меню copy/paste.
     */
    private fun disableSelectionMenu() {
        super.setCustomSelectionActionModeCallback(object : ActionMode.Callback {
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onDestroyActionMode(mode: ActionMode?) {}
        })
        super.setLongClickable(false)
    }

    private companion object {
        private const val DEFAULT_PIN_LENGTH = 4
        private const val MIN_PIN_LENGTH = 3
        private const val MAX_PIN_LENGTH = 8
        private const val DEFAULT_ITEM_WIDTH = 40f
        private const val DEFAULT_ITEM_HEIGHT = 48f
        private const val DEFAULT_ITEM_CORNER_RADIUS = 4f
        private const val DEFAULT_ITEM_BORDER_WIDTH = 1f
        private const val DEFAULT_SELECTED_ITEM_BORDER_WIDTH = DEFAULT_ITEM_BORDER_WIDTH + 1f
        private const val DEFAULT_ITEM_SPACING = 12f
    }
}