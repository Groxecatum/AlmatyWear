//package ysklyarov.almatywear.model
//
//import android.content.Context
//import android.graphics.Color
//import android.support.v7.widget.RecyclerView.ViewHolder
//import android.support.v7.widget.RecyclerView
//import ysklyarov.almatywear.R
//
//import ysklyarov.almatywear.YurisWatchface
//import ysklyarov.almatywear.config.ConfigAdapter
//
//import java.util.ArrayList
//
//
//object ConfigData {
//
//    /**
//     * Returns Watch Face Service class associated with configuration Activity.
//     */
//    val watchFaceServiceClass: Class<*>
//        get() = YurisWatchface::class.java
//
//    /**
//     * Interface all ConfigItems must implement so the [RecyclerView]'s Adapter associated
//     * with the configuration activity knows what type of ViewHolder to inflate.
//     */
//    interface ConfigItemType {
//        val configType: Int
//    }
//
//    /**
//     * Includes all data to populate each of the 5 different custom
//     * [ViewHolder] types in [AnalogComplicationConfigRecyclerViewAdapter].
//     */
//    fun getDataToPopulateAdapter(context: Context): ArrayList<ConfigItemType> {
//
//        val settingsConfigData = ArrayList<ConfigItemType>()
//
//        // Data for watch face preview and complications UX in settings Activity.
//        val complicationConfigItem = PreviewAndComplicationsConfigItem(R.drawable.add_complication)
//        settingsConfigData.add(complicationConfigItem)
//
//        return settingsConfigData
//    }
//
//    /**
//     * Data for Watch Face Preview with Complications Preview item in RecyclerView.
//     */
//    class PreviewAndComplicationsConfigItem internal constructor(val defaultComplicationResourceId: Int) : ConfigItemType {
//
//        override val configType: Int
//            get() = ConfigAdapter.TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG
//    }
//
//    /**
//     * Data for "more options" item in RecyclerView.
//     */
//    class MoreOptionsConfigItem internal constructor(val iconResourceId: Int) : ConfigItemType {
//
//        override val configType: Int
//            get() = ConfigAdapter.TYPE_MORE_OPTIONS
//    }
//
//    /**
//     * Data for color picker item in RecyclerView.
//     */
//    class ColorConfigItem internal constructor(
//            val name: String) : ConfigItemType {
//
//        override val configType: Int
//            get() = ConfigAdapter.TYPE_COLOR_CONFIG
//    }
//
//    /**
//     * Data for background image complication picker item in RecyclerView.
//     */
//    class BackgroundComplicationConfigItem internal constructor(
//            val name: String,
//            val iconResourceId: Int) : ConfigItemType {
//
//        override val configType: Int
//            get() = ConfigAdapter.TYPE_BACKGROUND_COMPLICATION_IMAGE_CONFIG
//    }
//}