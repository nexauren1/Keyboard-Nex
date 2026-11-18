import 'package:material_ui/material_ui.dart';
import 'package:flutter/services.dart';

import 'core/keyboard_bridge.dart';

const Color _bg = Color(0xFF090B16);
const Color _surface = Color(0xFF15192B);
const Color _surface2 = Color(0xFF1D2340);
const Color _muted = Color(0xFF9DA6C2);
const Color _violet = Color(0xFF8968FF);
const Color _cyan = Color(0xFF42D8E8);

class KeyboardNexShell extends StatefulWidget {
  const KeyboardNexShell({super.key});

  @override
  State<KeyboardNexShell> createState() => _KeyboardNexShellState();
}

class _KeyboardNexShellState extends State<KeyboardNexShell> {
  int _index = 0;
  String _themeName = 'Neon Violet';
  Map<String, dynamic> _state = <String, dynamic>{};

  @override
  void initState() {
    super.initState();
    _refresh();
  }

  Future<void> _refresh() async {
    final value = await KeyboardBridge.getState();
    if (!mounted) return;
    setState(() {
      _state = value;
      _themeName = value['themeName']?.toString() ?? _themeName;
    });
  }

  void _goTo(int index) {
    setState(() => _index = index);
    if (index == 2) _refresh();
  }

  @override
  Widget build(BuildContext context) {
    final pages = <Widget>[
      HomeScreen(state: _state, onNavigate: _goTo, onRefresh: _refresh),
      ThemeGalleryScreen(
        selectedTheme: _themeName,
        onSelected: (name) async {
          await KeyboardBridge.setTheme(name);
          if (!mounted) return;
          setState(() => _themeName = name);
          _refresh();
        },
      ),
      ClipboardScreen(onChanged: _refresh),
      SettingsScreen(state: _state, onChanged: _refresh),
    ];

    return Scaffold(
      backgroundColor: _bg,
      body: IndexedStack(index: _index, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: _goTo,
        backgroundColor: const Color(0xFF111526),
        indicatorColor: _violet.withValues(alpha: 0.22),
        destinations: const <NavigationDestination>[
          NavigationDestination(icon: Icon(Icons.grid_view_rounded), label: 'Início'),
          NavigationDestination(icon: Icon(Icons.palette_outlined), label: 'Temas'),
          NavigationDestination(icon: Icon(Icons.content_paste_rounded), label: 'Clipboard'),
          NavigationDestination(icon: Icon(Icons.tune_rounded), label: 'Definições'),
        ],
      ),
    );
  }
}

class HomeScreen extends StatelessWidget {
  const HomeScreen({required this.state, required this.onNavigate, required this.onRefresh, super.key});

  final Map<String, dynamic> state;
  final ValueChanged<int> onNavigate;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    final clips = state['clips'] is List ? (state['clips'] as List).length : 0;
    final pinned = state['clips'] is List
        ? (state['clips'] as List).where((item) => item is Map && item['pinned'] == true).length
        : 0;
    final theme = state['themeName']?.toString() ?? 'Neon Violet';

    return SafeArea(
      child: RefreshIndicator(
        onRefresh: onRefresh,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 18, 20, 28),
          children: <Widget>[
            Row(
              children: <Widget>[
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(14),
                    gradient: const LinearGradient(colors: <Color>[Color(0xFF9876FF), Color(0xFF4B9CFF)]),
                  ),
                  child: const Icon(Icons.keyboard_alt_rounded, color: Colors.white),
                ),
                const SizedBox(width: 12),
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text('NEXAUREN  /  INPUT SYSTEM', style: TextStyle(color: _muted, fontSize: 10, letterSpacing: 1.3, fontWeight: FontWeight.w700)),
                      SizedBox(height: 3),
                      Text('Keyboard Nex', style: TextStyle(fontSize: 21, fontWeight: FontWeight.w800)),
                    ],
                  ),
                ),
                IconButton(onPressed: onRefresh, icon: const Icon(Icons.refresh_rounded, color: _muted)),
              ],
            ),
            const SizedBox(height: 22),
            Container(
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(26),
                gradient: const LinearGradient(
                  colors: <Color>[Color(0xFF372A78), Color(0xFF202B5C), Color(0xFF123D55)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                boxShadow: <BoxShadow>[
                  BoxShadow(color: _violet.withValues(alpha: 0.14), blurRadius: 26, offset: const Offset(0, 12)),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      const Expanded(
                        child: Text('A TUA ESCRITA.\nA TUA IDENTIDADE.', style: TextStyle(fontSize: 19, height: 1.25, fontWeight: FontWeight.w900)),
                      ),
                      SizedBox(width: 130, child: _MiniKeyboard(accent: _violet, base: const Color(0xFF0C1023))),
                    ],
                  ),
                  const SizedBox(height: 15),
                  const Text('Personaliza o teclado, organiza os teus textos e escreve ao teu estilo.', style: TextStyle(color: Color(0xFFD8DDF4), fontSize: 13, height: 1.45)),
                  const SizedBox(height: 17),
                  Wrap(
                    spacing: 9,
                    runSpacing: 9,
                    children: <Widget>[
                      FilledButton.icon(
                        style: FilledButton.styleFrom(backgroundColor: const Color(0xFFF5F2FF), foregroundColor: const Color(0xFF22194A)),
                        onPressed: () => KeyboardBridge.showInputMethodPicker(),
                        icon: const Icon(Icons.keyboard_rounded, size: 18),
                        label: const Text('Selecionar teclado'),
                      ),
                      OutlinedButton(
                        style: OutlinedButton.styleFrom(foregroundColor: Colors.white, side: const BorderSide(color: Color(0x66FFFFFF))),
                        onPressed: () => onNavigate(1),
                        child: const Text('Ver temas'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 23),
            const _SectionHeading(title: 'VISÃO GERAL', subtitle: 'O teu espaço de escrita'),
            const SizedBox(height: 12),
            Row(
              children: <Widget>[
                Expanded(child: _MetricCard(icon: Icons.content_paste_rounded, title: 'Textos guardados', value: clips.toString(), tint: _cyan)),
                const SizedBox(width: 10),
                Expanded(child: _MetricCard(icon: Icons.push_pin_rounded, title: 'Fixados', value: pinned.toString(), tint: const Color(0xFFFFC66D))),
              ],
            ),
            const SizedBox(height: 10),
            _MetricCard(icon: Icons.palette_outlined, title: 'Tema ativo', value: theme, tint: _violet, wide: true),
            const SizedBox(height: 24),
            const _SectionHeading(title: 'ACESSO RÁPIDO', subtitle: 'Ferramentas principais'),
            const SizedBox(height: 12),
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: 10,
              crossAxisSpacing: 10,
              childAspectRatio: 1.65,
              children: <Widget>[
                _QuickAction(icon: Icons.palette_rounded, title: 'Galeria de temas', detail: 'Cores e estilos', tint: _violet, onTap: () => onNavigate(1)),
                _QuickAction(icon: Icons.library_books_rounded, title: 'Área de transferência', detail: 'Fixar e organizar', tint: _cyan, onTap: () => onNavigate(2)),
                _QuickAction(icon: Icons.language_rounded, title: 'Idiomas', detail: 'Layouts e escrita', tint: const Color(0xFF69D6A3), onTap: () => onNavigate(3)),
                _QuickAction(icon: Icons.settings_suggest_rounded, title: 'Preferências', detail: 'Som, vibração e mais', tint: const Color(0xFFFFB86B), onTap: () => onNavigate(3)),
              ],
            ),
            const SizedBox(height: 20),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(20), border: Border.all(color: Colors.white.withValues(alpha: 0.05))),
              child: const Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Icon(Icons.shield_moon_outlined, color: _cyan, size: 22),
                  SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text('Privacidade em primeiro lugar', style: TextStyle(fontWeight: FontWeight.w700)),
                        SizedBox(height: 5),
                        Text('O histórico do Keyboard Nex é guardado localmente. Campos de palavra-passe e conteúdos assinalados como sensíveis são excluídos da captura.', style: TextStyle(color: _muted, fontSize: 12, height: 1.45)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 22),
            const Center(child: Text('NEXAUREN  •  BUILDING DIGITAL PRODUCTS', style: TextStyle(color: Color(0xFF68718C), fontSize: 9, letterSpacing: 1.1, fontWeight: FontWeight.w700))),
          ],
        ),
      ),
    );
  }
}

class ThemeGalleryScreen extends StatelessWidget {
  const ThemeGalleryScreen({required this.selectedTheme, required this.onSelected, super.key});

  final String selectedTheme;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) {
    const themes = <_ThemeOption>[
      _ThemeOption('Neon Violet', 'Roxo elétrico e acabamento futurista', Color(0xFF8D69FF), Color(0xFF21173E), Color(0xFF0D0B1A)),
      _ThemeOption('Midnight Blue', 'Azul profundo para uma escrita discreta', Color(0xFF4D83FF), Color(0xFF14264B), Color(0xFF081326)),
      _ThemeOption('Ocean Cyan', 'Tons oceânicos com detalhes luminosos', Color(0xFF27C5D9), Color(0xFF103744), Color(0xFF071B25)),
      _ThemeOption('Minimal Light', 'Visual claro, limpo e elegante', Color(0xFF66728E), Color(0xFFE8ECF5), Color(0xFFD2D9E9), light: true),
    ];

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 28),
        children: <Widget>[
          const _PageHeading(eyebrow: 'PERSONALIZAÇÃO', title: 'Galeria de temas', subtitle: 'Escolhe a aparência que combina com a tua forma de escrever.'),
          const SizedBox(height: 18),
          for (final theme in themes) ...<Widget>[
            _ThemeCard(theme: theme, selected: selectedTheme == theme.name, onTap: () => onSelected(theme.name)),
            const SizedBox(height: 12),
          ],
          Container(
            padding: const EdgeInsets.all(17),
            decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(22), border: Border.all(color: _violet.withValues(alpha: 0.28))),
            child: const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Row(
                  children: <Widget>[
                    Icon(Icons.image_search_rounded, color: _violet),
                    SizedBox(width: 9),
                    Expanded(child: Text('Temas com fotografia', style: TextStyle(fontWeight: FontWeight.w800))),
                    _SmallTag(label: 'PRÓXIMA FASE', color: _violet),
                  ],
                ),
                SizedBox(height: 9),
                Text('A próxima galeria terá fundos fotográficos, pré-visualização, downloads e controlos para ajustar a imagem atrás das teclas.', style: TextStyle(color: _muted, fontSize: 12, height: 1.5)),
                SizedBox(height: 8),
                Text('Os pacotes serão alojados pela Nexauren com os direitos de utilização verificados.', style: TextStyle(color: Color(0xFFB7C1DC), fontSize: 11, height: 1.4)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class ClipboardScreen extends StatefulWidget {
  const ClipboardScreen({required this.onChanged, super.key});

  final Future<void> Function() onChanged;

  @override
  State<ClipboardScreen> createState() => _ClipboardScreenState();
}

class _ClipboardScreenState extends State<ClipboardScreen> {
  final TextEditingController _search = TextEditingController();
  final Set<String> _selected = <String>{};
  List<KeyboardClip> _clips = <KeyboardClip>[];
  String _filter = 'Todos';
  bool _loading = true;
  bool _working = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    if (mounted) setState(() => _loading = true);
    final state = await KeyboardBridge.getState();
    final raw = state['clips'];
    final items = raw is List
        ? raw.whereType<Map>().map(KeyboardClip.fromMap).where((clip) => clip.id.isNotEmpty).toList()
        : <KeyboardClip>[];
    if (!mounted) return;
    setState(() {
      _clips = items;
      _selected.removeWhere((id) => !_clips.any((clip) => clip.id == id));
      _loading = false;
    });
    await widget.onChanged();
  }

  List<KeyboardClip> get _visibleClips {
    final query = _search.text.trim().toLowerCase();
    return _clips.where((clip) {
      final matchesSearch = query.isEmpty || clip.text.toLowerCase().contains(query);
      final matchesFilter = _filter == 'Todos' ||
          (_filter == 'Fixados' && clip.pinned) ||
          (_filter == 'Recentes' && !clip.pinned);
      return matchesSearch && matchesFilter;
    }).toList();
  }

  Future<void> _perform(Future<void> Function() action, String success) async {
    if (_working) return;
    setState(() => _working = true);
    try {
      await action();
      await _load();
      if (mounted) _message(success);
    } on PlatformException catch (error) {
      if (mounted) _message(error.message ?? 'Não foi possível concluir a operação.');
    } finally {
      if (mounted) setState(() => _working = false);
    }
  }

  void _message(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message), behavior: SnackBarBehavior.floating));
  }

  Future<void> _toggleSelectedPin() async {
    final chosen = _clips.where((clip) => _selected.contains(clip.id)).toList();
    if (chosen.isEmpty) return;
    final pin = chosen.any((clip) => !clip.pinned);
    await _perform(() => KeyboardBridge.setClipsPinned(_selected.toList(), pin), pin ? 'Itens fixados.' : 'Fixação removida.');
    if (mounted) setState(() => _selected.clear());
  }

  Future<void> _deleteSelected() async {
    if (_selected.isEmpty) return;
    final accepted = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Eliminar itens selecionados?'),
        content: Text(_selected.length.toString() + ' item(ns) serão removidos do histórico.'),
        actions: <Widget>[
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Eliminar')),
        ],
      ),
    );
    if (accepted != true) return;
    await _perform(() => KeyboardBridge.deleteClips(_selected.toList()), 'Itens eliminados.');
    if (mounted) setState(() => _selected.clear());
  }

  Future<void> _clearAll() async {
    if (_clips.isEmpty) return;
    final accepted = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Limpar todo o histórico?'),
        content: const Text('Todos os textos guardados, incluindo os fixados, serão eliminados.'),
        actions: <Widget>[
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Limpar tudo')),
        ],
      ),
    );
    if (accepted == true) {
      await _perform(KeyboardBridge.clearClips, 'Histórico limpo.');
      if (mounted) setState(() => _selected.clear());
    }
  }

  Future<void> _edit(KeyboardClip clip) async {
    final controller = TextEditingController(text: clip.text);
    final updated = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Editar texto'),
        content: TextField(controller: controller, minLines: 3, maxLines: 8, autofocus: true, decoration: const InputDecoration(hintText: 'Texto guardado')),
        actions: <Widget>[
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(context, controller.text.trim()), child: const Text('Guardar')),
        ],
      ),
    );
    controller.dispose();
    if (updated == null || updated.isEmpty || updated == clip.text) return;
    await _perform(() => KeyboardBridge.updateClip(clip.id, updated), 'Texto atualizado.');
  }

  Future<void> _copy(KeyboardClip clip) async {
    await Clipboard.setData(ClipboardData(text: clip.text));
    if (mounted) _message('Texto copiado para a área de transferência do Android.');
  }

  @override
  Widget build(BuildContext context) {
    final visible = _visibleClips;
    return SafeArea(
      child: Column(
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 10),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Row(
                  children: <Widget>[
                    const Expanded(child: _PageHeading(eyebrow: 'PRODUTIVIDADE', title: 'Área de transferência', subtitle: 'Organiza os textos que guardaste no teclado.')),
                    IconButton(onPressed: _load, icon: const Icon(Icons.refresh_rounded), tooltip: 'Atualizar'),
                    IconButton(onPressed: _clearAll, icon: const Icon(Icons.delete_sweep_outlined), tooltip: 'Limpar tudo'),
                  ],
                ),
                const SizedBox(height: 14),
                TextField(
                  controller: _search,
                  onChanged: (_) => setState(() {}),
                  decoration: InputDecoration(
                    hintText: 'Pesquisar textos guardados',
                    prefixIcon: const Icon(Icons.search_rounded),
                    filled: true,
                    fillColor: _surface,
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide.none),
                    suffixIcon: _search.text.isEmpty ? null : IconButton(onPressed: () { _search.clear(); setState(() {}); }, icon: const Icon(Icons.close_rounded)),
                  ),
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 7,
                  children: <Widget>[
                    for (final label in <String>['Todos', 'Fixados', 'Recentes'])
                      ChoiceChip(label: Text(label), selected: _filter == label, onSelected: (_) => setState(() => _filter = label)),
                  ],
                ),
              ],
            ),
          ),
          if (_selected.isNotEmpty)
            Container(
              margin: const EdgeInsets.fromLTRB(20, 0, 20, 8),
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
              decoration: BoxDecoration(color: _surface2, borderRadius: BorderRadius.circular(14), border: Border.all(color: _violet.withValues(alpha: 0.35))),
              child: Row(
                children: <Widget>[
                  Text(_selected.length.toString() + ' selecionado(s)', style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 12)),
                  const Spacer(),
                  TextButton(onPressed: () => setState(() => _selected.clear()), child: const Text('Cancelar')),
                  IconButton(onPressed: _toggleSelectedPin, tooltip: 'Fixar/desafixar', icon: const Icon(Icons.push_pin_outlined, color: _violet)),
                  IconButton(onPressed: _deleteSelected, tooltip: 'Eliminar selecionados', icon: const Icon(Icons.delete_outline_rounded, color: Color(0xFFFF7D8B))),
                ],
              ),
            ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : visible.isEmpty
                    ? _EmptyClipboard(hasItems: _clips.isNotEmpty, onRefresh: _load)
                    : RefreshIndicator(
                        onRefresh: _load,
                        child: ListView.builder(
                          padding: const EdgeInsets.fromLTRB(20, 4, 20, 22),
                          itemCount: visible.length + 1,
                          itemBuilder: (context, index) {
                            if (index == 0) {
                              return Row(
                                children: <Widget>[
                                  Text(visible.length.toString() + ' texto(s)', style: const TextStyle(color: _muted, fontSize: 11)),
                                  const Spacer(),
                                  TextButton(
                                    onPressed: () => setState(() {
                                      if (_selected.length == visible.length) {
                                        _selected.clear();
                                      } else {
                                        _selected.addAll(visible.map((clip) => clip.id));
                                      }
                                    }),
                                    child: Text(_selected.length == visible.length && visible.isNotEmpty ? 'Desmarcar' : 'Selecionar todos'),
                                  ),
                                ],
                              );
                            }
                            final clip = visible[index - 1];
                            return _ClipboardCard(
                              clip: clip,
                              selected: _selected.contains(clip.id),
                              onSelected: (value) => setState(() => value ? _selected.add(clip.id) : _selected.remove(clip.id)),
                              onCopy: () => _copy(clip),
                              onEdit: () => _edit(clip),
                              onPin: () => _perform(
                                () => KeyboardBridge.setClipsPinned(<String>[clip.id], !clip.pinned),
                                clip.pinned ? 'Item desafixado.' : 'Item fixado.',
                              ),
                              onDelete: () => _perform(() => KeyboardBridge.deleteClips(<String>[clip.id]), 'Item eliminado.'),
                            );
                          },
                        ),
                      ),
          ),
          if (_working) const LinearProgressIndicator(minHeight: 2, color: _violet, backgroundColor: _surface),
        ],
      ),
    );
  }
}

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({required this.state, required this.onChanged, super.key});

  final Map<String, dynamic> state;
  final Future<void> Function() onChanged;

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _autoCapture = true;
  bool _haptics = true;
  bool _sound = false;
  int _ttl = 3600000;
  String _height = 'normal';

  @override
  void initState() {
    super.initState();
    _readValues();
  }

  @override
  void didUpdateWidget(covariant SettingsScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.state != widget.state) _readValues();
  }

  void _readValues() {
    final state = widget.state;
    _autoCapture = state['autoCapture'] != false;
    _haptics = state['hapticsEnabled'] != false;
    _sound = state['soundEnabled'] == true;
    _ttl = (state['clipTtlMs'] as num?)?.toInt() ?? 3600000;
    _height = state['keyboardHeight']?.toString() ?? 'normal';
  }

  Future<void> _save(String key, Object value) async {
    try {
      await KeyboardBridge.setSetting(key, value);
      await widget.onChanged();
    } on PlatformException catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(error.message ?? 'Não foi possível guardar a definição.')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    const ttlOptions = <int, String>{
      300000: '5 minutos',
      900000: '15 minutos',
      1800000: '30 minutos',
      3600000: '1 hora',
      86400000: '24 horas',
      604800000: '7 dias',
    };

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 28),
        children: <Widget>[
          const _PageHeading(eyebrow: 'CONTROLO DO TECLADO', title: 'Definições', subtitle: 'Ajusta o comportamento do Keyboard Nex.'),
          const SizedBox(height: 20),
          const _SectionHeading(title: 'ÁREA DE TRANSFERÊNCIA', subtitle: 'Controlo do histórico local'),
          const SizedBox(height: 10),
          _PreferenceCard(
            child: SwitchListTile(
              contentPadding: EdgeInsets.zero,
              value: _autoCapture,
              onChanged: (value) {
                setState(() => _autoCapture = value);
                _save('auto_capture', value);
              },
              title: const Text('Guardar textos automaticamente', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13)),
              subtitle: const Text('Guarda textos copiados quando o teclado está ativo.', style: TextStyle(color: _muted, fontSize: 11)),
              activeThumbColor: _violet,
            ),
          ),
          const SizedBox(height: 9),
          _PreferenceCard(
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 3),
              child: DropdownButtonFormField<int>(
                value: ttlOptions.containsKey(_ttl) ? _ttl : 3600000,
                decoration: const InputDecoration(labelText: 'Apagar textos não fixados após', border: InputBorder.none),
                items: ttlOptions.entries.map((entry) => DropdownMenuItem<int>(value: entry.key, child: Text(entry.value))).toList(),
                onChanged: (value) {
                  if (value == null) return;
                  setState(() => _ttl = value);
                  _save('clip_ttl_ms', value);
                },
              ),
            ),
          ),
          const SizedBox(height: 8),
          const Text('Os textos fixados permanecem guardados até serem removidos manualmente. O Android pode aplicar as suas próprias regras à área de transferência do sistema.', style: TextStyle(color: _muted, fontSize: 11, height: 1.45)),
          const SizedBox(height: 22),
          const _SectionHeading(title: 'TECLADO', subtitle: 'Sensação e dimensão das teclas'),
          const SizedBox(height: 10),
          _PreferenceCard(
            child: SwitchListTile(
              contentPadding: EdgeInsets.zero,
              value: _haptics,
              onChanged: (value) {
                setState(() => _haptics = value);
                _save('haptics_enabled', value);
              },
              title: const Text('Vibração ao pressionar', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13)),
              subtitle: const Text('Resposta tátil ao tocar nas teclas.', style: TextStyle(color: _muted, fontSize: 11)),
              activeThumbColor: _violet,
            ),
          ),
          const SizedBox(height: 9),
          _PreferenceCard(
            child: SwitchListTile(
              contentPadding: EdgeInsets.zero,
              value: _sound,
              onChanged: (value) {
                setState(() => _sound = value);
                _save('sound_enabled', value);
              },
              title: const Text('Som das teclas', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13)),
              subtitle: const Text('Som discreto ao escrever.', style: TextStyle(color: _muted, fontSize: 11)),
              activeThumbColor: _violet,
            ),
          ),
          const SizedBox(height: 9),
          _PreferenceCard(
            child: DropdownButtonFormField<String>(
              value: _height,
              decoration: const InputDecoration(labelText: 'Altura do teclado', border: InputBorder.none),
              items: const <DropdownMenuItem<String>>[
                DropdownMenuItem(value: 'compact', child: Text('Compacta')),
                DropdownMenuItem(value: 'normal', child: Text('Padrão')),
                DropdownMenuItem(value: 'tall', child: Text('Alta')),
              ],
              onChanged: (value) {
                if (value == null) return;
                setState(() => _height = value);
                _save('keyboard_height', value);
              },
            ),
          ),
          const SizedBox(height: 22),
          const _SectionHeading(title: 'CONFIGURAÇÃO DO ANDROID', subtitle: 'Ativa o Keyboard Nex no sistema'),
          const SizedBox(height: 10),
          _ActionSetting(
            icon: Icons.settings_input_antenna_rounded,
            title: 'Ativar ou gerir teclados',
            subtitle: 'Abre as definições de métodos de entrada do Android.',
            onTap: () => KeyboardBridge.openInputMethodSettings(),
          ),
          const SizedBox(height: 9),
          _ActionSetting(
            icon: Icons.keyboard_alt_outlined,
            title: 'Selecionar Keyboard Nex',
            subtitle: 'Abre o seletor de teclado do Android.',
            onTap: () => KeyboardBridge.showInputMethodPicker(),
          ),
          const SizedBox(height: 22),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(18)),
            child: const Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Icon(Icons.lock_outline_rounded, color: _cyan),
                SizedBox(width: 11),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text('Privacidade e armazenamento', style: TextStyle(fontWeight: FontWeight.w700)),
                      SizedBox(height: 5),
                      Text('O histórico gerido pelo Keyboard Nex é armazenado localmente. O teclado exclui campos de palavra-passe e respeita os indicadores de conteúdo sensível fornecidos pelo Android.', style: TextStyle(color: _muted, fontSize: 11, height: 1.5)),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 18),
          const Center(child: Text('KEYBOARD NEX  •  NEXAUREN', style: TextStyle(color: Color(0xFF68718C), letterSpacing: 1.1, fontSize: 9, fontWeight: FontWeight.w700))),
        ],
      ),
    );
  }
}

class _ThemeOption {
  const _ThemeOption(this.name, this.description, this.accent, this.keyColor, this.baseColor, {this.light = false});

  final String name;
  final String description;
  final Color accent;
  final Color keyColor;
  final Color baseColor;
  final bool light;
}

class _ThemeCard extends StatelessWidget {
  const _ThemeCard({required this.theme, required this.selected, required this.onTap});

  final _ThemeOption theme;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final foreground = theme.light ? const Color(0xFF1B2440) : Colors.white;
    return InkWell(
      borderRadius: BorderRadius.circular(21),
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(13),
        decoration: BoxDecoration(
          color: _surface,
          borderRadius: BorderRadius.circular(21),
          border: Border.all(color: selected ? theme.accent : Colors.white.withValues(alpha: 0.06), width: selected ? 1.5 : 1),
        ),
        child: Column(
          children: <Widget>[
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(13),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(15),
                gradient: LinearGradient(colors: <Color>[theme.keyColor, theme.baseColor], begin: Alignment.topLeft, end: Alignment.bottomRight),
              ),
              child: _MiniKeyboard(accent: theme.accent, base: theme.baseColor, light: theme.light),
            ),
            const SizedBox(height: 12),
            Row(
              children: <Widget>[
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(theme.name, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 14)),
                      const SizedBox(height: 3),
                      Text(theme.description, style: const TextStyle(color: _muted, fontSize: 11)),
                    ],
                  ),
                ),
                if (selected)
                  const _SmallTag(label: 'ATIVO', color: _cyan)
                else
                  Icon(Icons.radio_button_unchecked_rounded, color: foreground.withValues(alpha: 0.55)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _MiniKeyboard extends StatelessWidget {
  const _MiniKeyboard({required this.accent, required this.base, this.light = false});

  final Color accent;
  final Color base;
  final bool light;

  @override
  Widget build(BuildContext context) {
    final key = light ? const Color(0xFFF5F7FC) : const Color(0xFF28304B);
    final label = light ? const Color(0xFF26304A) : const Color(0xFFE8ECFF);
    const rows = <String>['qwertyuiop', 'asdfghjkl', 'zxcvbnm'];
    return Container(
      padding: const EdgeInsets.all(5),
      decoration: BoxDecoration(color: base.withValues(alpha: 0.55), borderRadius: BorderRadius.circular(10)),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          for (final row in rows)
            Padding(
              padding: const EdgeInsets.only(bottom: 4),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  for (final letter in row.split(''))
                    Expanded(
                      child: Container(
                        margin: const EdgeInsets.symmetric(horizontal: 1),
                        height: 23,
                        decoration: BoxDecoration(color: key, borderRadius: BorderRadius.circular(5)),
                        alignment: Alignment.center,
                        child: Text(letter, style: TextStyle(color: label, fontSize: 8, fontWeight: FontWeight.w700)),
                      ),
                    ),
                ],
              ),
            ),
          Row(
            children: <Widget>[
              _miniKey('?123', accent, label, flex: 2),
              _miniKey('☺', key, label),
              _miniKey('Português', key, label, flex: 5),
              _miniKey('↵', accent, label, flex: 2),
            ],
          ),
        ],
      ),
    );
  }

  Widget _miniKey(String text, Color color, Color label, {int flex = 1}) {
    return Expanded(
      flex: flex,
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 1),
        height: 22,
        decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(5)),
        alignment: Alignment.center,
        child: Text(text, maxLines: 1, overflow: TextOverflow.clip, style: TextStyle(color: label, fontSize: 7, fontWeight: FontWeight.w700)),
      ),
    );
  }
}

class _ClipboardCard extends StatelessWidget {
  const _ClipboardCard({
    required this.clip,
    required this.selected,
    required this.onSelected,
    required this.onCopy,
    required this.onEdit,
    required this.onPin,
    required this.onDelete,
  });

  final KeyboardClip clip;
  final bool selected;
  final ValueChanged<bool> onSelected;
  final VoidCallback onCopy;
  final VoidCallback onEdit;
  final VoidCallback onPin;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final expiry = clip.pinned ? 'Fixado · não expira automaticamente' : _expiryDescription(clip.expiresAt);
    return Container(
      margin: const EdgeInsets.only(bottom: 9),
      padding: const EdgeInsets.fromLTRB(8, 8, 4, 8),
      decoration: BoxDecoration(
        color: selected ? const Color(0xFF292342) : _surface,
        borderRadius: BorderRadius.circular(17),
        border: Border.all(color: selected ? _violet.withValues(alpha: 0.65) : Colors.white.withValues(alpha: 0.05)),
      ),
      child: Row(
        children: <Widget>[
          Checkbox(value: selected, onChanged: (value) => onSelected(value ?? false), activeColor: _violet),
          Expanded(
            child: InkWell(
              onTap: onCopy,
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(clip.text, maxLines: 3, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, height: 1.4)),
                    const SizedBox(height: 5),
                    Row(
                      children: <Widget>[
                        Icon(clip.pinned ? Icons.push_pin_rounded : Icons.schedule_rounded, size: 12, color: clip.pinned ? const Color(0xFFFFC66D) : _muted),
                        const SizedBox(width: 4),
                        Expanded(child: Text(expiry, style: const TextStyle(color: _muted, fontSize: 10))),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert_rounded, color: _muted),
            onSelected: (value) {
              if (value == 'edit') onEdit();
              if (value == 'pin') onPin();
              if (value == 'delete') onDelete();
            },
            itemBuilder: (context) => <PopupMenuEntry<String>>[
              const PopupMenuItem(value: 'edit', child: Text('Editar')),
              PopupMenuItem(value: 'pin', child: Text(clip.pinned ? 'Desafixar' : 'Fixar')),
              const PopupMenuItem(value: 'delete', child: Text('Eliminar')),
            ],
          ),
        ],
      ),
    );
  }

  String _expiryDescription(int? expiresAt) {
    if (expiresAt == null) return 'Validade definida nas definições';
    final remaining = expiresAt - DateTime.now().millisecondsSinceEpoch;
    if (remaining <= 0) return 'Expira ao atualizar o histórico';
    if (remaining < 3600000) return 'Expira em ' + (remaining / 60000).ceil().toString() + ' min';
    if (remaining < 86400000) return 'Expira em ' + (remaining / 3600000).ceil().toString() + ' h';
    return 'Expira em ' + (remaining / 86400000).ceil().toString() + ' dia(s)';
  }
}

class _EmptyClipboard extends StatelessWidget {
  const _EmptyClipboard({required this.hasItems, required this.onRefresh});

  final bool hasItems;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Container(width: 66, height: 66, decoration: BoxDecoration(color: _surface2, borderRadius: BorderRadius.circular(22)), child: const Icon(Icons.content_paste_off_rounded, color: _violet, size: 30)),
            const SizedBox(height: 16),
            Text(hasItems ? 'Nenhum resultado' : 'Ainda não há textos guardados', style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
            const SizedBox(height: 7),
            Text(hasItems ? 'Experimenta outro termo ou filtro.' : 'Copia um texto enquanto utilizas o Keyboard Nex. Depois, ele aparecerá aqui para poderes fixar, editar ou eliminar.', textAlign: TextAlign.center, style: const TextStyle(color: _muted, fontSize: 12, height: 1.5)),
            const SizedBox(height: 14),
            OutlinedButton.icon(onPressed: onRefresh, icon: const Icon(Icons.refresh_rounded), label: const Text('Atualizar')),
          ],
        ),
      ),
    );
  }
}

class _PageHeading extends StatelessWidget {
  const _PageHeading({required this.eyebrow, required this.title, required this.subtitle});

  final String eyebrow;
  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(eyebrow, style: const TextStyle(color: _violet, fontSize: 10, letterSpacing: 1.35, fontWeight: FontWeight.w800)),
        const SizedBox(height: 7),
        Text(title, style: const TextStyle(fontSize: 25, height: 1.15, fontWeight: FontWeight.w900)),
        const SizedBox(height: 7),
        Text(subtitle, style: const TextStyle(color: _muted, fontSize: 12, height: 1.45)),
      ],
    );
  }
}

class _SectionHeading extends StatelessWidget {
  const _SectionHeading({required this.title, required this.subtitle});

  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(title, style: const TextStyle(fontSize: 11, letterSpacing: 1.2, fontWeight: FontWeight.w800, color: Color(0xFFC0B4FF))),
        const SizedBox(height: 4),
        Text(subtitle, style: const TextStyle(fontSize: 11, color: _muted)),
      ],
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({required this.icon, required this.title, required this.value, required this.tint, this.wide = false});

  final IconData icon;
  final String title;
  final String value;
  final Color tint;
  final bool wide;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(18), border: Border.all(color: Colors.white.withValues(alpha: 0.04))),
      child: Row(
        children: <Widget>[
          Container(width: 36, height: 36, decoration: BoxDecoration(color: tint.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(12)), child: Icon(icon, color: tint, size: 19)),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(title, style: const TextStyle(color: _muted, fontSize: 10)),
                const SizedBox(height: 4),
                Text(value, maxLines: 1, overflow: TextOverflow.ellipsis, style: TextStyle(fontSize: wide ? 14 : 18, fontWeight: FontWeight.w900)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _QuickAction extends StatelessWidget {
  const _QuickAction({required this.icon, required this.title, required this.detail, required this.tint, required this.onTap});

  final IconData icon;
  final String title;
  final String detail;
  final Color tint;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(17),
      child: Container(
        padding: const EdgeInsets.all(13),
        decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(17), border: Border.all(color: Colors.white.withValues(alpha: 0.04))),
        child: Row(
          children: <Widget>[
            Icon(icon, color: tint, size: 23),
            const SizedBox(width: 9),
            Expanded(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(title, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 11), maxLines: 1, overflow: TextOverflow.ellipsis),
                  const SizedBox(height: 3),
                  Text(detail, style: const TextStyle(color: _muted, fontSize: 9), maxLines: 1, overflow: TextOverflow.ellipsis),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PreferenceCard extends StatelessWidget {
  const _PreferenceCard({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 5),
      decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(16), border: Border.all(color: Colors.white.withValues(alpha: 0.04))),
      child: child,
    );
  }
}

class _ActionSetting extends StatelessWidget {
  const _ActionSetting({required this.icon, required this.title, required this.subtitle, required this.onTap});

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: _surface,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: <Widget>[
              Icon(icon, color: _violet, size: 22),
              const SizedBox(width: 12),
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: <Widget>[
                Text(title, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700)),
                const SizedBox(height: 3),
                Text(subtitle, style: const TextStyle(color: _muted, fontSize: 10, height: 1.35)),
              ])),
              const Icon(Icons.chevron_right_rounded, color: _muted),
            ],
          ),
        ),
      ),
    );
  }
}

class _SmallTag extends StatelessWidget {
  const _SmallTag({required this.label, required this.color});

  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      decoration: BoxDecoration(color: color.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(30), border: Border.all(color: color.withValues(alpha: 0.32))),
      child: Text(label, style: TextStyle(color: color, fontSize: 8, letterSpacing: 0.8, fontWeight: FontWeight.w900)),
    );
  }
}
