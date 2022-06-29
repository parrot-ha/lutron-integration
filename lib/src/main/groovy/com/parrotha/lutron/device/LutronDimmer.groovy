/*
 * Copyright (c) 2021-2022 by the respective copyright holders.
 * All rights reserved.
 * <p>
 * This file is part of Parrot Home Automation Hub Lutron Extension.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.parrotha.lutron.device

import com.parrotha.device.HubAction
import com.parrotha.device.Protocol

import java.text.DecimalFormat

metadata {
    definition(name: "Lutron Dimmer",
            namespace: "com.parrotha.lutron.device",
            author: "Parrot HA",
            tags: "LUTRON") {
        capability "Switch"
        capability "SwitchLevel"
    }
}

def parse(String description) {
    log.debug("Lutron Dimmer parse method received: $description")

    String[] message = description.split(",")

    if (message.length > 3 && message[2] == "1") {
        if (message[3].toDouble() > 0.0) {
            sendEvent(name: "switch", value: "on")
        } else {
            sendEvent(name: "switch", value: "off")
        }
        sendEvent(name: "level", value: message[3].toDouble().intValue())
    }
}

def on() {
    return new HubAction("#OUTPUT,${device.deviceNetworkId},1,100.0", Protocol.OTHER)
}

def off() {
    return new HubAction("#OUTPUT,${device.deviceNetworkId},1,0.0", Protocol.OTHER)
}

def setLevel(value) {
    return new HubAction("#OUTPUT,${device.deviceNetworkId},1,${new DecimalFormat("#.0").format(value.toDouble())}", Protocol.OTHER)
}
