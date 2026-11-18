import 'package:material_ui/material_ui.dart';
import 'screens.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const KeyboardNexApp());
}

class KeyboardNexApp extends StatelessWidget {
  const KeyboardNexApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Keyboard Nex',
      debugShowCheckedModeBanner: false,
      themeMode: ThemeMode.dark,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF8968FF),
          brightness: Brightness.dark,
        ),
        scaffoldBackgroundColor: const Color(0xFF090B16),
        fontFamily: 'Roboto',
      ),
      home: const KeyboardNexShell(),
    );
  }
}
