/**
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
package com.parrotha.integration.lutron;

import com.parrotha.device.HubAction;
import com.parrotha.device.HubResponse;
import com.parrotha.integration.DeviceIntegration;
import com.parrotha.integration.extension.ItemAddIntegrationExtension;
import com.parrotha.integration.extension.ItemListIntegrationExtension;
import com.parrotha.ui.PreferencesBuilder;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetInputListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LutronIntegration extends DeviceIntegration implements TelnetInputListener, ItemAddIntegrationExtension, ItemListIntegrationExtension {
    private static final Logger logger = LoggerFactory.getLogger(LutronIntegration.class);

    private TelnetClient tc;

    private boolean running = false;

    private static final List<String> tags = List.of("LUTRON");

    @Override
    public Map<String, Object> itemListButton(String id, String button) {
        return null;
    }

    @Override
    public List<Map<String, Object>> getItemListLayout() {
        return null;
    }

    @Override
    public List<Map<String, Object>> getItemList() {
        return null;
    }

    @Override
    public boolean itemAdd(Map itemSettings) {
        return false;
    }

    @Override
    public boolean itemUpdate(String id, Map deviceSettings) {
        return false;
    }

    @Override
    public Map<String, Object> getItemAddLayout() {
        return null;
    }

    @Override
    public boolean itemDelete(String id) {
        return false;
    }

    @Override
    public void telnetInputAvailable() {
        new Thread(() -> {
            processInput(readInput());
        }).start();
    }

    private void processInput(String input) {
        if (input == null) {
            return;
        }
        if (input.trim().equals("login:")) {
            try {
                tc.getOutputStream().write("lutron\n".getBytes(StandardCharsets.UTF_8));
                tc.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (input.trim().equals("password:")) {
            try {
                tc.getOutputStream().write("integration\n".getBytes(StandardCharsets.UTF_8));
                tc.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (input.length() > 0) {
                // extract message
                final int startIndex = input.indexOf('~');
                final int endIndex = input.indexOf("\r\n");
                if (startIndex >= 0 && endIndex > startIndex) {
                    input = input.substring(startIndex, endIndex);
                    String[] message = input.split(",");
                    String integrationId = message[1];
                    sendDeviceMessage(integrationId, input);
                }
            }
        }
    }

    @Override
    public void start() {
        String bridgeAddress = getSettingAsString("bridgeAddress");
        if (bridgeAddress != null && bridgeAddress.length() > 0) {
            tc = new TelnetClient("VT100");
            tc.registerInputListener(this);

            try {
                tc.connect(bridgeAddress, 23);
                running = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String readInput() {
        final InputStream instr = tc.getInputStream();
        try {
            final byte[] buff = new byte[1024];
            int ret_read = 0;

            if (instr.available() > 0) {
                ret_read = instr.read(buff);
                if (ret_read > 0) {
                    String value = new String(buff, 0, ret_read);
                    return value;
                }
            }
        } catch (final IOException e) {
            System.err.println("Exception while reading socket:" + e.getMessage());
        }
        return null;
    }

    @Override
    public void stop() {
        try {
            running = false;
            if (tc != null) {
                tc.unregisterInputListener();
                tc.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getPreferencesLayout() {
        return new PreferencesBuilder()
                .withTextInput("bridgeAddress",
                        "Lutron Bridge Address",
                        "IP Address or URL for the Lutron bridge.",
                        true,
                        true)
                .build();
    }

    @Override
    public void settingValueChanged(List<String> keys) {
        if (logger.isDebugEnabled()) {
            logger.debug("values changed " + keys);
        }
        if (keys.contains("bridgeAddress")) {
            // restart the integration
            this.stop();
            this.start();
        }
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public String getName() {
        return "Lutron";
    }

    @Override
    public String getDescription() {
        return "Allows integration of Lutron based devices.";
    }

    @Override
    public Map<String, String> getDisplayInformation() {
        Map<String, String> model = new HashMap<>();
        if (tc != null) {
            //model.put("IP Address", ipAddress != null ? ipAddress : "Not Set");
            model.put("Status", running ? "RUNNING" : "STOPPED");
        } else {
            model.put("Status", "STOPPED");
        }
        return model;
    }

    @Override
    public boolean removeIntegrationDevice(String deviceNetworkId) {
        return true;
    }

    @Override
    public HubResponse processAction(HubAction hubAction) {
        try {
            if (tc != null && tc.getOutputStream() != null) {
                tc.getOutputStream().write((hubAction.getAction() + "\r\n").getBytes(StandardCharsets.UTF_8));
                tc.getOutputStream().flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
