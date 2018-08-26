package ysklyarov.almatywear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v4.content.ContextCompat
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationHelperActivity
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.SurfaceHolder
import android.view.WindowInsets
import android.widget.Toast
import org.jetbrains.anko.doAsync
import java.io.BufferedReader
import java.io.InputStreamReader

import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.log

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

//    private val LEFT_COMPLICATION_ID = 0
//    private val RIGHT_COMPLICATION_ID = 1

//    val displayMetrics = DisplayMetrics()

    //val windowManager = WindowManager.defaultDisplay()

//    private val COMPLICATION_IDS = intArrayOf(LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID)

//    private IntArray COMPLICATION_SUPPORTED_TYPES = {
//       {
//           ComplicationData.TYPE_RANGED_VALUE,
//           ComplicationData.TYPE_ICON,
//           ComplicationData.TYPE_SHORT_TEXT,
//           ComplicationData.TYPE_SMALL_IMAGE
//       },
//       {
//           ComplicationData.TYPE_RANGED_VALUE,
//           ComplicationData.TYPE_ICON,
//           ComplicationData.TYPE_SHORT_TEXT,
//           ComplicationData.TYPE_SMALL_IMAGE
//       }
//    }

    companion object {
        private val NORMAL_TYPEFACE = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

        /**
         * Updates rate in milliseconds for interactive mode. We update once a second since seconds
         * are displayed in interactive mode.
         */
        private const val INTERACTIVE_UPDATE_RATE_MS = 1000

        /**
         * Handler message id for updating the time periodically in interactive mode.
         */
        private const val MSG_UPDATE_TIME = 0
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

        private lateinit var mBackgroundPaint: Paint
        private lateinit var mTextPaint: Paint
        private lateinit var mTextOnay: Paint
        private lateinit var mTextKopilka: Paint

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false
        private var mAmbient: Boolean = false
        private var isRound: Boolean = true
        private var needToRelocateNumbers: Boolean = false

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

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@YurisWatchface)
                    .setAcceptsTapEvents(true)
                    .build())

            mCalendar = Calendar.getInstance()

            val resources = this@YurisWatchface.resources
            mYOffset = resources.getDimension((R.dimen.digital_y_offset))
            mYOffsetLower = resources.getDimension((R.dimen.digital_y_offset_lower))
            mYOffsetUpper = resources.getDimension((R.dimen.digital_y_offset_upper))

            // Initializes background.
            mBackgroundPaint = Paint().apply {
                color = ContextCompat.getColor(applicationContext, R.color.background)
            }

            mTextPaint = Paint().apply {
                typeface = NORMAL_TYPEFACE
                isAntiAlias = true
                color = ContextCompat.getColor(applicationContext, R.color.digital_text)
            }

            mTextOnay = Paint().apply {
                typeface = NORMAL_TYPEFACE
                isAntiAlias = true
                color = ContextCompat.getColor(applicationContext, R.color.onay_text)
            }

            mTextKopilka = Paint().apply {
                typeface = NORMAL_TYPEFACE
                isAntiAlias = true
                color = ContextCompat.getColor(applicationContext, R.color.kopilka_text)
            }
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            mLowBitAmbient = properties.getBoolean(
                    WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false)
            mBurnInProtection = properties.getBoolean(
                    WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode
            needToRelocateNumbers = true

            if (mLowBitAmbient) {
                mTextPaint.isAntiAlias = !inAmbientMode
                mTextKopilka.isAntiAlias = !inAmbientMode
                mTextOnay.isAntiAlias = !inAmbientMode
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer()
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
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

                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            canvas.drawColor(Color.BLACK)

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            val now = System.currentTimeMillis()
            mCalendar.timeInMillis = now
            if (needToRelocateNumbers) {
                determineXOffset()
                needToRelocateNumbers = false
            }

//            val resp = sendGet()
//            Log.d("SEND", "resp")

            val text = if (mAmbient)
                String.format("%d:%02d", mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE))
            else
                String.format("%d:%02d:%02d", mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE),
                        mCalendar.get(Calendar.SECOND))
            canvas.drawText(text, mXOffset, mYOffset, mTextPaint)
//            canvas.drawText("5000", mXOffsetOnay, mYOffsetLower, mTextOnay)
//            canvas.drawText("200000", mXOffsetKopilka, mYOffsetUpper, mTextKopilka)
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

        fun determineXOffset() {
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

            val subTextSize = resources.getDimension(
                    if (isRound)
                        R.dimen.digital_subtext_size_round
                    else
                        R.dimen.digital_subtext_size
            )

            mTextPaint.textSize = textSize
            mTextOnay.textSize = subTextSize
            mTextKopilka.textSize = subTextSize
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
