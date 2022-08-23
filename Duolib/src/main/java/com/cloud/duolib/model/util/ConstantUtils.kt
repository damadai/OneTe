package com.cloud.duolib.model.util

/**
 * um上报
 * */
const val UM_DUO_INIT_ORDER = "UM_DUO_INIT_ORDER"//"云机失败-初始化登录"

const val UM_DUO_CHOICE_SERVER = "UM_DUO_CHOICE_SERVER"//"云机-选择服务商"
const val UM_DUO_SERVER_USE = "UM_DUO_SERVER_USE"//"云机-使用服务商"

const val UM_DUO_MEDIA_FAIL = "UM_DUO_MEDIA_FAIL"//"云机失败-初始化媒体流"
const val UM_DUO_MEDIA_PLAY_FAIL = "UM_DUO_MEDIA_PLAY_FAIL"//"云机失败-播放媒体流"

const val UM_DUO_MEDIA_PAUSE_FAIL = "UM_DUO_MEDIA_PAUSE_FAIL"// "云机失败-暂停"

const val UM_DUO_APP_PUSH = "UM_DUO_APP_PUSH"// "云机选择-应用上传"
const val UM_DUO_APP_PUSH_SPEED = "UM_DUO_APP_PUSH_SPEED"// "云机选择-应用上传加速"
const val UM_DUO_APP_PUSH_LOCAL = "UM_DUO_APP_PUSH_LOCAL"// "云机选择-应用上传成功"
const val UM_DUO_APP_PUSH_HANDLE = "UM_DUO_APP_PUSH_HANDLE"// "云机选择-应用加速刷新"

/**
 * 失败提示
 * */
const val FAIL_CLOUD_GET_DEVICE_LIST = "获取列表失败"
const val FAIL_CLOUD_DUO_UN_SUPPORT = "不支持"

/**
 * 激励时间间隔
 * */
const val PREFERENCE_REWARD_INTERVAL = "PREFERENCE_REWARD_INTERVAL"

const val PREFERENCE_SHOW_GUIDE = "PREFERENCE_SHOW_GUIDE"

/**
 * 状态码释义
 * */
const val LOG_SUC = 77200//成功数据
const val LOG_FAIL = 77400//错误信息
const val LOG_ERROR = 77500//错误响应
const val LOG_EXCEPT = 77600//错误捕获

/**
 * 多多云回调结果
 * */
const val DUO_SUCCESS_REQUEST = "请求成功"
const val DUO_ERROR_REQUEST = "请求错误"
const val DUO_SUCCESS_EMPTY = "数据为空"
const val DUO_ERROR_DATA = "推送错误"

/**
 * 文件上传
 * */
const val OPEN_ZAR_PKG = "ru.zdevs.zarchiver.pro"
const val OPEN_ZAR_ACT = "ru.zdevs.zarchiver.pro.ZArchiver"
const val OPEN_ZAR_SUC = "(刚刚传的在【文件管理】APP偏底部，下滑↓看看\uD83D\uDC47)"
const val OPEN_ZAR_FAIL = "请打开【文件管理】APP查看"

const val BROADCAST_FILE_PUSH_START = "com.cloud.duolib.BROADCAST_FILE_PUSH_START"//开始
const val BROADCAST_FILE_PUSH_END = "com.cloud.duolib.BROADCAST_FILE_PUSH_END"//结束

const val RECEIVER_FILE_PUSH_FILE_STATUS = "RECEIVER_FILE_PUSH_FILE_STATUS"//状态
const val RECEIVER_FILE_PUSH_FILE_PATH = "RECEIVER_FILE_PUSH_FILE_PATH"//路径