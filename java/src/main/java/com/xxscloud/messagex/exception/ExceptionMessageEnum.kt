package com.xxscloud.messagex.exception

enum class ExceptionMessageEnum(val code: String, val message: String) {
    DOCTORS_REGISTERED("341", "手机号已注册"),
    PATIENT_REGISTERED("342", "手机号已注册"),
    GROUP_NOT_NULL("225", "分组下面存在用户，请先删除用户或移动用户在删除该分组"),
    UPLOAD_ERROR("230", "文件上传失败"),
    DOCTORS_NULL("224", "白名单医生不存在，请检查手机号"),
    DATA_NULL("330", "数据不存在"),
    FLOW_NULL("222", "流水号找不到"),
    TRANSFER_P_P_ERROR("221", "不能给自己转账"),
    PASSWORD_ERROR("220", "密码错误"),
    PARENT_NULL("229", "代付款人不是您的上级用户"),
    BALANCE_NOT("228", "余额不够"),
    BANK_CARD_NO_EXIST("227", "银行卡不存在"),
    BANK_CARD_EXIST("227", "您已经绑过卡了"),
    STATUS_ERROR("226", "状态错误"),
    ORDER_COMPLETE_PAY("225", "订单已经完成支付"),
    ZZXP_ID_ERROR("560", "自在小铺ID错误"),
    ZZXP_PARENT_NULL("2224", "没有邀请码,无法注册"),
    CHANNEL_NULL("223", "渠道错误"),
    ADDRESS_NULL("223", "地址错误"),
    ORDER_NULL("223", "订单错误"),
    COUPON_ERROR("568", "优惠券代码错误"),
    ORDER_REFUND_EXIST("223", "订单退款已经存在"),
    PAY_SUPPORT("221", "支付不支持"),
    ACTIVITY_CATEGORY_NULL("222", "活动种类为空"),
    ACTIVITY_NULL("222", "活动代码为空"),
    ARRAYS_MAX_LENGTH("222", "数组长度超过最大长度"),
    ARRAYS_IS_NULL("222", "数组是空的"),
    USER_ADDRESS_COUNT_MAX("222", "用户地址超过最大"),
    GOODS_NULL("222", "商品不存在"),
    USER_REPORT_NULL("2222", "您还没有注册"),
    USER_REPORT_NO_MATCH("2222", "抱歉~, 报表是其他人的微信的"),
    WE_CHAT_CODE_ERROR("2322", "微信代码错误"),
    ACCOUNT_NULL("2312", "账号未注册"),
    ACCOUNT_EXIST("2312", "账号已存在"),
    ACCOUNT_OR_PASSWORD_ERROR("2312", "账号密码不正确"),
    OSS_ID_ERROR("5672", "资源Id错误"),
    OSS_ERROR("5671", "上传失败"),
    NOT_PAY("5688", "未支付"),
    REPORT_NULL("5671", "找不到报表"),
    TOKEN_FAILURE("9998", "令牌过期"),
    TERMINAL_ID_ERROR("2001", "终端ID错误"),
    SAVE_REPORT_NULL("2002", "报表数据为空保存失败"),
    REDIS_CODE_NULL("2001", "验证码已经过期，请重新获取"),
    CODE_ERROR("2001", "验证码错误"),
    SEND_FAIL("2001", "短信发送失败"),
    TERMINAL_NULL("1001", "终端设备不存在"),
    TERMINAL_NOT_ONLINE("1002", "终端不在线"),
    USER_NULL("1003", "用户不存在"),
    SAVE_ERROR("1004", "保存数据失败"),
    REMOVER_ERROR("1005", "数据移除失败"),
    SAVE_ERROR_REPEAT("1005", "数据已经经保存，请勿重复提交")
}