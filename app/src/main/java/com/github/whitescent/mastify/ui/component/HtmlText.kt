package com.github.whitescent.mastify.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.util.fastForEach
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.whitescent.mastify.network.model.emoji.Emoji
import com.github.whitescent.mastify.ui.theme.AppTheme
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.util.regex.Pattern

private const val URL_TAG = "url_tag"
private const val ID_IMAGE = "image"
private val emojiRegex = "(?<=:)(.*?)(?=:)".toRegex() // Everything between ':' and ':' non inclusive

@Composable
fun HtmlText(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = AppTheme.colors.primaryContent,
  softWrap: Boolean = true,
  overflow: TextOverflow = TextOverflow.Clip,
  maxLines: Int = Int.MAX_VALUE,
  onTextLayout: (TextLayoutResult) -> Unit = {},
  onLinkClick: ((String) -> Unit)? = null,
  fontSize: TextUnit = 14.sp,
  fontWeight: FontWeight = FontWeight.Normal,
  style: TextStyle = LocalTextStyle.current.copy(
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight
  ),
  urlSpanStyle: SpanStyle = SpanStyle(
    color = Color(0xFF0079D3),
    textDecoration = TextDecoration.None
  )
) {
  val document = remember(text) { Jsoup.parse(text.replace("\n", "<br>")) }
  val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
  val value = remember(document) { buildContentAnnotatedString(document, urlSpanStyle, style) }
  Text(
    text = value,
    modifier = modifier.pointerInput(Unit) {
      awaitEachGesture {
        val change = awaitFirstDown()
        val annotation =
          layoutResult.value?.getOffsetForPosition(change.position)?.let {
            value.getStringAnnotations(start = it, end = it).firstOrNull()
          }
        annotation?.let {
          if (change.pressed != change.previousPressed) change.consume()
          val up = waitForUpOrCancellation()?.also {
            if (it.pressed != it.previousPressed) it.consume()
          }
          up?.let {
            onLinkClick?.invoke(annotation.item)
          }
        }
      }
    },
    onTextLayout = {
      layoutResult.value = it
      onTextLayout(it)
    },
    fontSize = fontSize,
    style = style,
    maxLines = maxLines,
    overflow = overflow,
    softWrap = softWrap,
    inlineContent = mapOf(
      ID_IMAGE to InlineTextContent(
        Placeholder(
          width = style.fontSize,
          height = style.fontSize,
          placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        )
      ) { target ->
        AsyncImage(
          model = target,
          contentDescription = null
        )
      }
    )
  )
}

private fun buildContentAnnotatedString(
  document: Document,
  urlSpanStyle: SpanStyle,
  textStyle: TextStyle,
): AnnotatedString {
  return buildAnnotatedString {
    document.body().childNodes().forEach {
      renderNode(it, urlSpanStyle, textStyle)
    }
  }
}

private fun AnnotatedString.Builder.renderNode(
  node: Node,
  urlSpanStyle: SpanStyle,
  textStyle: TextStyle,
) {
  when (node) {
    is Element -> renderElement(node, urlSpanStyle, textStyle)
    is TextNode -> {
      pushStyle(textStyle.toSpanStyle())
      append(node.wholeText)
      pop()
    }
  }
}

private fun AnnotatedString.Builder.renderElement(
  element: Element,
  urlSpanStyle: SpanStyle,
  textStyle: TextStyle
) {
  if (skipElement(element = element)) return
  when (element.normalName()) {
    "a" -> {
      val href = element.attr("href")
      pushStringAnnotation(tag = URL_TAG, annotation = href)
      withStyle(urlSpanStyle) {
        append(element.text())
      }
      pop()
    }

    "br" -> append("\n")

    "span", "p" -> {
      element.childNodes().forEach {
        renderNode(node = it, urlSpanStyle, textStyle)
      }
    }

    "emoji" -> renderEmoji(element)
  }
}

private fun AnnotatedString.Builder.renderEmoji(element: Element) {
  val emojiHref = element.attr("target")
  appendInlineContent(ID_IMAGE, emojiHref)
}

private fun skipElement(element: Element): Boolean = element.hasClass("invisible")

fun annotateInlineEmojis(
  text: String,
  shortcodes: List<String>,
  to: AnnotatedString.Builder,
) {
  val emojiPositions = emojiRegex.findAll(text).filter { shortcodes.contains(it.value) }
  text.forEachIndexed { index: Int, c: Char ->
    val emojiPosition =
      emojiPositions.find {
        it.range.any { rangeElement -> rangeElement in index - 1..index + 1 }
      }
    // Account for custom emoji ':' parenthesis
    if (emojiPosition?.range?.first == index) {
      emojiPosition.also {
        to.appendInlineContent(emojiPosition.value)
      }
    }
    if (emojiPosition == null) {
      to.append(c)
    }
  }
}

fun generateHtmlContentWithEmoji(
  content: String,
  emojis: List<Emoji>
): String {
  var result = content
  emojis.forEach { (shortcode, url) ->
    val regex = Pattern.compile(":$shortcode:", Pattern.LITERAL).toRegex()
    result = result.replace(regex = regex) {
      "<emoji class=\"emoji\" target=\"$url\">:$shortcode:</emoji>"
    }
  }
  return result
}

@Composable
fun inlineTextContentWithEmoji(
  emojis: List<Emoji>,
  size: TextUnit = LocalTextStyle.current.fontSize,
): Map<String, InlineTextContent> {
  return buildMap {
    emojis.fastForEach { emoji ->
      put(
        emoji.shortcode,
        InlineTextContent(
          placeholder = Placeholder(
            width = size.takeOrElse { 14.sp },
            height = size.takeOrElse { 14.sp },
            PlaceholderVerticalAlign.TextCenter,
          ),
          children = {
            AsyncImage(
              model = ImageRequest.Builder(LocalContext.current)
                .data(emoji.staticUrl)
                .crossfade(true)
                .build(),
              contentDescription = emoji.shortcode,
              modifier = Modifier.fillMaxSize(),
            )
          },
        ),
      )
    }
  }
}
