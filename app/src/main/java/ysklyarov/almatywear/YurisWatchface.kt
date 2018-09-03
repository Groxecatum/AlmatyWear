package ysklyarov.almatywear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.content.ContextCompat
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.rendering.ComplicationDrawable
import android.support.wearable.complications.rendering.TextRenderer
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.text.Layout
import android.text.TextPaint
import android.util.Log
import android.util.SparseArray
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.WindowInsets
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 *
 *
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
class YurisWatchface : CanvasWatchFaceService() {

    companion object {
        private val TAG = "YurisWear"

        private val NORMAL_TYPEFACE = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

        private const val INTERACTIVE_UPDATE_RATE_MS = 1000

        private const val MSG_UPDATE_TIME = 0

        private val BACKGROUND_COMPLICATION_ID = 0

        private val LEFT_COMPLICATION_ID = 100
        private val RIGHT_COMPLICATION_ID = 101

        private val COMPLICATION_IDS = intArrayOf(/*BACKGROUND_COMPLICATION_ID,*/ LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID)

        private val COMPLICATION_SUPPORTED_TYPES = arrayOf(
                //intArrayOf(ComplicationData.TYPE_LARGE_IMAGE),
                intArrayOf(ComplicationData.TYPE_RANGED_VALUE, ComplicationData.TYPE_ICON, ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE),
                intArrayOf(ComplicationData.TYPE_RANGED_VALUE, ComplicationData.TYPE_ICON, ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE))

        // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to check if complication location
        // is supported in settings config activity.
//        fun getComplicationId(
//                complicationLocation: ConfigAdapter.ComplicationLocation): Int {
//            // Add any other supported locations here.
//            when (complicationLocation) {
//                //ConfigAdapter.ComplicationLocation.BACKGROUND -> return BACKGROUND_COMPLICATION_ID
//                ConfigAdapter.ComplicationLocation.LEFT -> return LEFT_COMPLICATION_ID
//                ConfigAdapter.ComplicationLocation.RIGHT -> return RIGHT_COMPLICATION_ID
//                else -> return -1
//            }
//        }
//
//        // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to retrieve all complication ids.
//        fun getComplicationIds(): IntArray {
//            return COMPLICATION_IDS
//        }
//
//        // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to see which complication types
//        // are supported in the settings config activity.
//        fun getSupportedComplicationTypes(
//                complicationLocation: ConfigAdapter.ComplicationLocation): IntArray {
//            // Add any other supported locations here.
//            when (complicationLocation) {
//                //ConfigAdapter.ComplicationLocation.BACKGROUND -> return COMPLICATION_SUPPORTED_TYPES[0]
//                ConfigAdapter.ComplicationLocation.LEFT -> return COMPLICATION_SUPPORTED_TYPES[1]
//                ConfigAdapter.ComplicationLocation.RIGHT -> return COMPLICATION_SUPPORTED_TYPES[2]
//                else -> return intArrayOf()
//            }
//        }
    }

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: YurisWatchface.Engine) : Handler() {
        private val mWeakReference: WeakReference<YurisWatchface.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = mWeakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private lateinit var mCalendar: Calendar

        private var mRegisteredTimeZoneReceiver = false

        private var mXOffset: Float = 0F
        private var mYOffset: Float = 0F
        private var mYOffsetLower: Float = 0F
        private var mYOffsetUpper: Float = 0F
        private var mXOffsetKopilka: Float = 0F
        private var mXOffsetOnay: Float = 0F

        //private lateinit var mOnayRenderer: TextRenderer
        //private lateinit var mKopilkaRenderer: TextRenderer

        private lateinit var mTextPaint: Paint
        private lateinit var mDimTextPaint: Paint
//        private lateinit var mTextOnay: Paint
//        private lateinit var mTextKopilka: Paint

        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false
        private var mAmbient: Boolean = false
        private var isRound: Boolean = true
        private var needToRelocateNumbers: Boolean = false

        private lateinit var mActiveComplicationDataSparseArray: SparseArray<ComplicationData>
        private lateinit var mComplicationDrawableSparseArray: SparseArray<ComplicationDrawable>

        private val mUpdateTimeHandler: Handler = EngineHandler(this)

        private val mTimeZoneReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        private fun sendGet(): String {
            val url = "http://www.google.com/"
            val obj = URL(url)

            with(obj.openConnection() as HttpURLConnection) {
                // optional default is GET
                requestMethod = "GET"


                println("\nSending 'GET' request to URL : $url")
                println("Response Code : $responseCode")

                BufferedReader(InputStreamReader(inputStream)).use {
                    val response = StringBuffer()

                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }
                    return response.toString()
                }
            }
        }

        private fun loadSavedPreferences() {
//            Log.d(TAG, "Loading preferences")
//            val markerColorResourceName = applicationContext.getString(R.string.saved_marker_color)

//            mWatchHandHighlightColor = mSharedPref.getInt(markerColorResourceName, Color.WHITE)

//            if (mBackgroundColor == Color.WHITE) {
//                mWatchHandAndComplicationsColor = Color.BLACK
//                mWatchHandShadowColor = Color.WHITE
//            } else {
//                mWatchHandAndComplicationsColor = Color.WHITE
//                mWatchHandShadowColor = Color.BLACK
//            }
//
//            val unreadNotificationPreferenceResourceName = applicationContext.getString(R.string.saved_unread_notifications_pref)
//
//            mUnreadNotificationsPreference = mSharedPref.getBoolean(unreadNotificationPreferenceResourceName, true)
        }

        private fun setComplicationsActiveAndAmbientColors(primaryComplicationColor: Int) {
//            Log.d(TAG, "Setting color for complications")
            var complicationId: Int
            var complicationDrawable: ComplicationDrawable

            for (i in COMPLICATION_IDS.indices) {
                complicationId = COMPLICATION_IDS[i]
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId)

                if (complicationId == BACKGROUND_COMPLICATION_ID) {
                    // It helps for the background color to be black in case the image used for the
                    // watch face's background takes some time to load.
                    complicationDrawable.setBackgroundColorActive(Color.BLACK)
                } else {
                    // Active mode colors.
                    complicationDrawable.setBorderColorActive(primaryComplicationColor)
                    complicationDrawable.setRangedValuePrimaryColorActive(primaryComplicationColor)

                    // Ambient mode colors.
                    complicationDrawable.setBorderColorAmbient(Color.WHITE)
                    complicationDrawable.setRangedValuePrimaryColorAmbient(Color.WHITE)
                }
            }
        }

        private fun initializeComplicationsAndBackground() {
            Log.d(TAG, "Initializing complications")

            mActiveComplicationDataSparseArray = SparseArray(COMPLICATION_IDS.size)

            val leftComplicationDrawable = ComplicationDrawable(applicationContext)

            val rightComplicationDrawable = ComplicationDrawable(applicationContext)

            //val backgroundComplicationDrawable = ComplicationDrawable(applicationContext)

            mComplicationDrawableSparseArray = SparseArray(COMPLICATION_IDS.size)

            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable)
            mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable)
//            mComplicationDrawableSparseArray.put(BACKGROUND_COMPLICATION_ID, backgroundComplicationDrawable)

            setComplicationsActiveAndAmbientColors(Color.WHITE)
            setActiveComplications(*COMPLICATION_IDS)
        }

        private fun initializeWatchFace() {
            val resources = this@YurisWatchface.resources
            mYOffset = resources.getDimension((R.dimen.digital_y_offset))
            mYOffsetLower = resources.getDimension((R.dimen.digital_y_offset_lower))
            mYOffsetUpper = resources.getDimension((R.dimen.digital_y_offset_upper))

            mTextPaint = Paint().apply {
                typeface = NORMAL_TYPEFACE
                isAntiAlias = true
                color = ContextCompat.getColor(applicationContext, R.color.digital_text)
            }

            mDimTextPaint = Paint().apply {
                typeface = NORMAL_TYPEFACE
                isAntiAlias = true
                color = ContextCompat.getColor(applicationContext, R.color.digital_text_dim)
            }


//            mOnayRenderer = TextRenderer()
//            mOnayRenderer.setAlignment(Layout.Alignment.ALIGN_CENTER)
//            mOnayRenderer.setGravity(Gravity.CENTER)
//            mOnayRenderer.setPaint(TextPaint().apply {
//                typeface = NORMAL_TYPEFACE
//                isAntiAlias = true
//                color = ContextCompat.getColor(applicationContext, R.color.onay_text)
//                textSize = R.dimen.digital_subtext_size.toFloat()
//            })
//            mOnayRenderer.setText("5000")

//            mTextKopilka = Paint().apply {
//                typeface = NORMAL_TYPEFACE
//                isAntiAlias = true
//                color = ContextCompat.getColor(applicationContext, R.color.kopilka_text)
//            }
        }

        override fun onCreate(holder: SurfaceHolder) {
//            Log.d(TAG, "Creating")
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@YurisWatchface)
                    .setAcceptsTapEvents(true)
                    .setHideNotificationIndicator(true)
                    .build())

            mCalendar = Calendar.getInstance()

            // Used throughout watch face to pull user's preferences.
//            val context = applicationContext
//            mSharedPref = context.getSharedPreferences(
//                    getString(R.string.digital_complication_preference_file_key),
//                    Context.MODE_PRIVATE)

            loadSavedPreferences()
            initializeComplicationsAndBackground()
            initializeWatchFace()
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            mLowBitAmbient = properties.getBoolean(WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false)
            mBurnInProtection = properties.getBoolean(WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)

            var complicationDrawable: ComplicationDrawable

            for (i in COMPLICATION_IDS.indices) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i])

                complicationDrawable.setLowBitAmbient(mLowBitAmbient)
                complicationDrawable.setBurnInProtection(mBurnInProtection)
            }
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode
            needToRelocateNumbers = true

//            mOnayRenderer.setInAmbientMode(inAmbientMode)

            if (mLowBitAmbient) {
                mTextPaint.isAntiAlias = !inAmbientMode
                mDimTextPaint.isAntiAlias = !inAmbientMode
//                mTextKopilka.isAntiAlias = !inAmbientMode
//                mTextOnay.isAntiAlias = !inAmbientMode
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            Log.d(TAG, "Setting bounds")
            /*
             * Calculates location bounds for right and left circular complications. Please note,
             * we are not demonstrating a long text complication in this watch face.
             *
             * We suggest using at least 1/4 of the screen width for circular (or squared)
             * complications and 2/3 of the screen width for wide rectangular complications for
             * better readability.
             */

            // For most Wear devices, width and height are the same, so we just chose one (width).
            val sizeOfComplication = width / 4
            val midpointOfScreen = width / 2

            val horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2
            val verticalOffset = midpointOfScreen - sizeOfComplication / 2

            val leftBounds =
            // Left, Top, Right, Bottom
                    Rect(
                            horizontalOffset,
                            verticalOffset,
                            horizontalOffset + sizeOfComplication,
                            verticalOffset + sizeOfComplication)

            Log.d(TAG, "Setting left bounds $horizontalOffset $verticalOffset, $sizeOfComplication")

            val leftComplicationDrawable = mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID)
            leftComplicationDrawable.bounds = leftBounds

            val rightBounds =
            // Left, Top, Right, Bottom
                    Rect(
                            midpointOfScreen + horizontalOffset,
                            verticalOffset,
                            midpointOfScreen + horizontalOffset + sizeOfComplication + 20,
                            verticalOffset + sizeOfComplication + 20)

            Log.d(TAG, "Setting right bounds from midpoint $midpointOfScreen - $horizontalOffset $verticalOffset, $sizeOfComplication")

            val rightComplicationDrawable = mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID)
            rightComplicationDrawable.bounds = rightBounds
        }

        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
//                    doAsync {
//                        val resp = sendGet()
//                        Log.d("SEND", resp)
//                    }
                }
                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                    // The user has started a different gesture or otherwise cancelled the tap.
                }
                WatchFaceService.TAP_TYPE_TAP -> {
                    for (i in COMPLICATION_IDS.indices.reversed()) {
                        val complicationId = COMPLICATION_IDS[i]
                        val complicationDrawable = mComplicationDrawableSparseArray.get(complicationId)

                        val successfulTap = complicationDrawable.onTap(x, y)

                        if (successfulTap) {
                            return
                        }
                    }
                }
            }
            invalidate()
        }

        fun drawBackground(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)
        }

        fun drawWatchFace(canvas: Canvas) {
            val now = System.currentTimeMillis()
            mCalendar.timeInMillis = now
            if (needToRelocateNumbers) {
                determineXOffset()
                needToRelocateNumbers = false
            }

//            val resp = sendGet()
//            Log.d("SEND", "resp")

            val paint = if (mAmbient)
                mDimTextPaint
            else
                mTextPaint

            val text = if (mAmbient)
                String.format("%d:%02d", mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE))
            else
                String.format("%d:%02d:%02d", mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE),
                        mCalendar.get(Calendar.SECOND))
            canvas.drawText(text, mXOffset, mYOffset, paint)
//            mOnayRenderer.draw(canvas, Rect(0, mYOffsetLower.toInt(), canvas.width,
//                    (mYOffsetLower + 20).toInt()))
        }

        fun drawComplications(canvas: Canvas) {
//            Log.d(TAG, "Drawing complications")
            var complicationId: Int
            var complicationDrawable: ComplicationDrawable

            for (i in COMPLICATION_IDS.indices) {
                complicationId = COMPLICATION_IDS[i]
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId)

                Log.d(TAG, "Drawing complication $i")

                complicationDrawable.draw(canvas)
            }
        }

        override fun onComplicationDataUpdate(
                complicationId: Int, complicationData: ComplicationData?) {
            Log.d(TAG, "onComplicationDataUpdate() id: $complicationId")

            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData)

            // Updates correct ComplicationDrawable with updated data.
            val complicationDrawable = mComplicationDrawableSparseArray.get(complicationId)
            complicationDrawable.setComplicationData(complicationData)

            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
//            Log.d(TAG, "Drawing")
            drawBackground(canvas)
            drawComplications(canvas)
            drawWatchFace(canvas)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()

                // Update time zone in case it changed while we weren't visible.
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer()
        }

        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@YurisWatchface.registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = false
            this@YurisWatchface.unregisterReceiver(mTimeZoneReceiver)
        }

        private fun determineXOffset() {
            if (mAmbient) {
                mXOffset = resources.getDimension(
                        if (isRound)
                            R.dimen.digital_x_offset_round_amb
                        else
                            R.dimen.digital_x_offset_amb
                )
            } else {
                mXOffset = resources.getDimension(
                        if (isRound)
                            R.dimen.digital_x_offset_round
                        else
                            R.dimen.digital_x_offset
                )
            }

        }

        override fun onApplyWindowInsets(insets: WindowInsets) {
            super.onApplyWindowInsets(insets)

            // Load resources that have alternate values for round watches.
            val resources = this@YurisWatchface.resources
            isRound = insets.isRound

            determineXOffset()

            mXOffsetKopilka = resources.getDimension(
                    if (isRound)
                        R.dimen.digital_x_offset_kopilka_round
                    else
                        R.dimen.digital_x_offset_kopilka)

            mXOffsetOnay = resources.getDimension(
                    if (isRound)
                        R.dimen.digital_x_offset_onay_round
                    else
                        R.dimen.digital_x_offset_onay)

            val textSize = resources.getDimension(
                    if (isRound)
                        R.dimen.digital_text_size_round
                    else
                        R.dimen.digital_text_size
            )

//            val subTextSize = resources.getDimension(
//                    if (isRound)
//                        R.dimen.digital_subtext_size_round
//                    else
//                        R.dimen.digital_subtext_size
//            )

            mTextPaint.textSize = textSize
            mDimTextPaint.textSize = textSize
//            mTextOnay.textSize = subTextSize
//            mTextKopilka.textSize = subTextSize
        }

        /**
         * Starts the [.mUpdateTimeHandler] timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private fun updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !isInAmbientMode
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }
    }
}
