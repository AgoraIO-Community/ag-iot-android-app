package io.agora.sdkwayang.util;

/**
 * @brief 定义了一些常量枚举值
 * @author luxiaohua@agora.io
 * @date 2023/06/08
 */

public class EnumClass {
    public enum ConnectStatus {
        /**
         * Wayang Server连接状态
         */
        DISCONNECT,
        CONNECTING,
        CONNECTED
    }

    /**
     * Wayang 消息种类
     */
    public enum CommandType {
        /**
         * 1: 执行SDK API
         */
        TYPE_1(1),
        /**
         * 2: APP 自定义API
         */
        TYPE_2(2),
        /**
         * 3: 其他自定义API
         */
        TYPE_3(3),
        /**
         * 4: SDK Callback
         */
        TYPE_4(4),
        TYPE_5(5),
        TYPE_6(6),
        TYPE_7(7),
        /**
         * 8: 自采集
         */
        TYPE_8(8),
        /**
         * 9: 自采集回调
         */
        TYPE_9(9),
        TYPE_10(10),
        TYPE_11(11),
        /**
         * 15: format error
         */
        TYPE_15(15);

        private int value = 0;

        CommandType(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }


    public enum ErrorType {
        /**
         * 15: 错误类型，TODO待梳理
         */
        TYPE_0,
        TYPE_1,
        TYPE_2,
        TYPE_3
    }
}
