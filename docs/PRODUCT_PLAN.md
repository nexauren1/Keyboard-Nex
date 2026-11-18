# Keyboard Nex — Plano de Produto e Funcionalidades

## 1. Referência competitiva: Gboard

A análise funcional do Gboard identifica estes grupos de capacidade a considerar:

- Digitação por deslize.
- Ditado por voz.
- Escrita manual onde suportada.
- Pesquisa de emojis e GIFs.
- Digitação multilingue, correção e sugestões.
- Tradução durante a escrita.
- Temas e fundos personalizados.
- Sons e vibração configuráveis.
- Gestos para mover o cursor e apagar texto.
- Modo de uma mão e opções de layout.
- Área de transferência com clips fixados.

Referências oficiais:
- https://play.google.com/store/apps/details?id=com.google.android.inputmethod.latin
- https://support.google.com/gboard/answer/6380730
- https://support.google.com/gboard/answer/2811346
- https://support.google.com/gboard/answer/7421372
- https://support.google.com/gboard/answer/6102154
- https://support.google.com/gboard/answer/10742542

## 2. Diferenciação Nexauren

A área de transferência deve ser um dos pontos fortes do Keyboard Nex:

- Histórico pesquisável.
- Selecionar um ou vários itens.
- Fixar/desafixar vários itens numa operação.
- Editar o conteúdo de um clip.
- Eliminar vários itens selecionados.
- Limpar o histórico completo.
- Configurar a validade dos itens não fixados.
- Manter os itens fixados até o utilizador os remover.
- Guardar o histórico localmente, sem sincronização por defeito.

A gestão em lote deve estar visível no próprio painel do teclado e não escondida num menu profundo.

## 3. Estrutura das telas

### A. Aplicação de configuração

- Início: estado do teclado e atalhos para ativar/selecionar.
- Idiomas: idioma, layout e alternância.
- Preferências: número fixo, altura, sons, vibração, símbolos e tecla emoji.
- Temas: categorias, miniaturas, pré-visualização e aplicar.
- Personalização: cor, brilho, opacidade, bordas e formato das teclas.
- Correções: sugestões, autocorreção, capitalização e pontuação.
- Dicionário pessoal: adicionar, editar, remover e importar/exportar.
- Atalhos de texto: abreviações e expansões.
- Privacidade: histórico, validade, dados sensíveis e modo privado.

### B. Teclado em contexto

- Barra superior de ferramentas personalizável.
- Linha de sugestões.
- Teclas QWERTY com estados pressionado/ativo.
- Painel de área de transferência.
- Painel de emojis e símbolos.
- Painel de ferramentas rápidas.
- Seletor/preview de temas.

## 4. Temas fotográficos

O objetivo é permitir que o utilizador descarregue um tema e o aplique sem aumentar demasiado o APK inicial.

Fluxo proposto:
1. O repositório Nexauren publica um catálogo leve de temas.
2. Cada tema tem miniatura, nome, categoria, tamanho e imagem de fundo.
3. O utilizador abre a pré-visualização e escolhe descarregar.
4. O app transfere apenas o tema selecionado.
5. O tema é guardado localmente e pode ser aplicado/removido.
6. O teclado compõe a imagem com a camada de teclas, contraste e efeitos.

Imagens Pixabay precisam ser integradas numa composição temática. A licença proíbe redistribuir o conteúdo como ficheiro autónomo e a API não permite hotlink permanente. Guardar os créditos e a origem dos ativos.

## 5. Experiência e efeitos

- Entrada e saída de painéis com fade/slide breve.
- Pressão de tecla com escala curta e retorno.
- Feedback háptico configurável.
- Transição de tema com pré-visualização antes de aplicar.
- Descarregamento com progresso, cancelamento e estado concluído.
- Estados visíveis para carregamento, erro, vazio e sucesso.
- Respeitar dispositivos de entrada de gama e permitir desativar animações.

## 6. Regras de privacidade

- Não recolher nem transmitir texto digitado.
- Não manter histórico quando o utilizador desativa a gravação.
- Ignorar conteúdo marcado como sensível.
- Não mostrar clips privados em pré-visualizações públicas.
- Limpar os dados expirados da área local.
- Dar ao utilizador um comando claro para apagar tudo.
- Não incluir permissão de Internet até existir um serviço de temas ou função online justificada.

## 7. Estado de implementação

Já codificado no repositório:
- Base Android nativa/Kotlin.
- Serviço IME registado no manifesto.
- Teclado QWERTY básico.
- Painel simples de emojis.
- Temas de cor iniciais.
- Histórico local de texto, fixação, edição, seleção múltipla e eliminação.
- Expiração configurável para clips não fixados.
- Aplicação de configuração inicial.

Ainda por implementar e testar:
- Motor de sugestões e autocorreção.
- Dicionários e alternância real entre idiomas.
- Glide typing.
- Ditado por voz.
- Tradução e pesquisa online de GIFs.
- Catálogo e download de temas fotográficos.
- Modo de uma mão/flutuante.
- Gestos de edição e movimento do cursor.
- Testes em vários aparelhos, tamanhos e versões Android.

Não anunciar nem publicar as funções da segunda lista como concluídas.
