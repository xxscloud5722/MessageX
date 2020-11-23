package com.xxscloud.messagex.config


import com.xxscloud.messagex.core.xxs.SnowFlake
import java.util.*

class Config {
    companion object {
        const val R_N_TICKET = "TICKET"
        const val R_N_ACTIVITY = "ACTIVITY:"
        const val R_N_REPORT = "REPORT:"
        const val R_N_REGISTERED = "REGISTERED:"
        const val R_N_TERMINAL_OPEN = "TERMINAL:OPEN:"
        const val R_N_TERMINAL = "TERMINAL:"
        const val R_N_USER_INFO = "TERMINAL:"
        const val R_N_PAY = "PAY:"
        const val R_N_API_LOGIN = "LOGIN:"

        const val R_N_GAME_TIME = "GAME_TIME_"
        const val R_N_GAME = "GAME_TURNTABLE_"
        const val R_N_GAME_PLAYER = "GAME_TURNTABLE_PLAYER_"
        const val R_N_GAME_TURNTABLE_ROOM = "GAME_TURNTABLE_ROOM:"
        const val R_N_GAME_NOTICE = "GAME_NOTICE:"
        const val GAME_AFK = "GAME_AFK"
        const val R_N_GAME_RESULT = "GAME_RESULT:"

        const val R_T_30M = 30 * 60 * 1000L
        const val R_T_2M = 2 * 60 * 1000L
        const val R_T_180M = 180 * 60L
        const val R_T_40S = 40L
        const val R_T_3M = 3 * 60L

        const val R_N_WX_USER_INFO = "WX:USER_INFO:"
        const val R_N_WX_CREATE_QR = "WX:CREATE_QR:"
        fun getId(): String {
            return SnowFlake.getId()
        }

        fun getGameId(): String {
            return "G" + SnowFlake.getId()
        }

        fun getRandomCode(): String {
            return String.format("%04d", Random().nextInt(9999))
        }

        fun getWarehouseUserId(): String {
            return "WU" + SnowFlake.getId()
        }

        fun getReportId(): String {
            return "R" + SnowFlake.getId()
        }

        fun getCustomerId(): String {
            return "CU" + SnowFlake.getId()
        }

        fun getResourcesId(): String {
            return "RS" + SnowFlake.getId()
        }

        fun getOrderNo(): String {
            return "D" + SnowFlake.getId()
        }

        fun getRefundOrderNo(): String {
            return "R" + SnowFlake.getId()
        }

        fun getPayFlowNo(): String {
            return "pay" + SnowFlake.getId()
        }

        fun getFundsFlow(): String {
            return "F" + SnowFlake.getId()
        }

        fun getWithdrawFlow(): String {
            return "W" + SnowFlake.getId()
        }

        fun parseEnv(value: String?): String? {
            if (value.isNullOrEmpty()) {
                return value
            }
            if (value.startsWith("$")) {
                return System.getenv(value.replace("$", "").replace("{", "").replace("}", ""))
            }
            return value
        }



        fun getValue(key: String, default: String = ""): String {
            return parseEnv(System.getProperty(key)) ?: default
        }

    }
}