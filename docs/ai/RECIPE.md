# Android IoT Video Calling App

## Overview

Connect an Android mobile client to an Agora IoT device for real-time audio/video calling and device-side media control.

Repository: [AgoraIO-Community/ag-iot-android-app](https://github.com/AgoraIO-Community/ag-iot-android-app)

## Prerequisites and Setup

1. Use Android Studio (the README recommends Dolphin 2021.3.1 Patch 1 or later) and a matching device SDK v2.1.2.
2. Obtain an Agora Link project App ID and provision a compatible IoT device.
3. Open the repository root in Android Studio, sync Gradle and run the app on an Android device.
4. Enter the App ID on first launch and add the IoT device using its NodeId. The device list subscribes to `PUBLIC_STREAM_1`.

## What To Check

- Confirm Connecting, Connected and Subscribed state transitions.
- Check device preview, audio mute, recording and screenshots.
- Disconnect/reconnect and verify stale device streams are released.

## Integration Notes

This app manages Agora Link devices. Use the matching IoT service and SDK, rather than a generic RTC channel-join workflow.

Keep App Certificates, customer secrets and privileged token generation on a trusted backend for production. Use only your own test credentials while configuring historical demos; do not commit them. Preserve the sample's existing dependency versions and lifecycle until a separate migration is tested.

## Source References

- [README.md](../../README.md)
- [app/build.gradle](../../app/build.gradle)

## Validation Status

This recipe was checked against the repository's source and setup documents. The platform build and live service/device flow have not been run as part of this documentation change. Complete the checks above in an environment with the required SDKs, hardware and service access before treating it as a validated deployment.

## Implementation Prompt

```text
You are working in AgoraIO-Community/ag-iot-android-app. Use docs/ai/RECIPE.md and its linked source files to adapt the Android IoT Video Calling App reference to this project.

Start by inspecting the actual sample directory and configuration described above. Resolve its listed missing prerequisites before claiming it is runnable. Preserve the existing architecture and compatible dependency versions. Implement the recipe's feature-specific flow, permissions and resource cleanup. Use a trusted backend for privileged credentials and tokens in production.

Run the repository-specific build or launch flow, then perform each What To Check item. Report the commands run, the observed results and anything blocked by service access, hardware or incomplete sample code. Do not claim live validation from a documentation or catalog build alone.
```
