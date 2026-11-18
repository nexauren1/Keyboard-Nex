import 'package:flutter/services.dart';

class KeyboardClip {
  const KeyboardClip({
    required this.id,
    required this.text,
    required this.createdAt,
    required this.expiresAt,
    required this.pinned,
  });

  final String id;
  final String text;
  final int createdAt;
  final int? expiresAt;
  final bool pinned;

  factory KeyboardClip.fromMap(Map<dynamic, dynamic> map) {
    return KeyboardClip(
      id: map['id']?.toString() ?? '',
      text: map['text']?.toString() ?? '',
      createdAt: (map['createdAt'] as num?)?.toInt() ?? 0,
      expiresAt: (map['expiresAt'] as num?)?.toInt(),
      pinned: map['pinned'] == true,
    );
  }
}

class KeyboardBridge {
  KeyboardBridge._();

  static const MethodChannel _channel =
      MethodChannel('com.nexauren.keyboardnex/bridge');

  static Future<Map<String, dynamic>> getState() async {
    try {
      final value =
          await _channel.invokeMapMethod<String, dynamic>('getState');
      return value ?? <String, dynamic>{};
    } on PlatformException {
      return <String, dynamic>{};
    } on MissingPluginException {
      return <String, dynamic>{};
    }
  }

  static Future<void> setTheme(String name) async {
    await _channel.invokeMethod<void>(
      'setTheme',
      <String, dynamic>{'name': name},
    );
  }

  static Future<void> setSetting(String key, Object value) async {
    await _channel.invokeMethod<void>(
      'setSetting',
      <String, dynamic>{'key': key, 'value': value},
    );
  }

  static Future<void> updateClip(String id, String text) async {
    await _channel.invokeMethod<void>(
      'updateClip',
      <String, dynamic>{'id': id, 'text': text},
    );
  }

  static Future<void> setClipsPinned(List<String> ids, bool pinned) async {
    await _channel.invokeMethod<void>(
      'setClipsPinned',
      <String, dynamic>{'ids': ids, 'pinned': pinned},
    );
  }

  static Future<void> deleteClips(List<String> ids) async {
    await _channel.invokeMethod<void>(
      'deleteClips',
      <String, dynamic>{'ids': ids},
    );
  }

  static Future<void> clearClips() async {
    await _channel.invokeMethod<void>('clearClips');
  }

  static Future<void> openInputMethodSettings() async {
    await _channel.invokeMethod<void>('openInputMethodSettings');
  }

  static Future<void> showInputMethodPicker() async {
    await _channel.invokeMethod<void>('showInputMethodPicker');
  }
}
