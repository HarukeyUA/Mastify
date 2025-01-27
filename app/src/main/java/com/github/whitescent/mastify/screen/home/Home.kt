/*
 * Copyright 2023 WhiteScent
 *
 * This file is a part of Mastify.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Mastify is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Mastify; if not,
 * see <http://www.gnu.org/licenses>.
 */

package com.github.whitescent.mastify.screen.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.whitescent.R
import com.github.whitescent.mastify.AppNavGraph
import com.github.whitescent.mastify.data.model.ui.StatusUiData
import com.github.whitescent.mastify.data.model.ui.StatusUiData.ReplyChainType.End
import com.github.whitescent.mastify.data.model.ui.StatusUiData.ReplyChainType.Null
import com.github.whitescent.mastify.data.repository.HomeRepository.Companion.FETCHNUMBER
import com.github.whitescent.mastify.data.repository.HomeRepository.Companion.PAGINGTHRESHOLD
import com.github.whitescent.mastify.mapper.status.getReplyChainType
import com.github.whitescent.mastify.mapper.status.hasUnloadedParent
import com.github.whitescent.mastify.paging.LoadState
import com.github.whitescent.mastify.screen.destinations.PostDestination
import com.github.whitescent.mastify.screen.destinations.ProfileDestination
import com.github.whitescent.mastify.screen.destinations.StatusDetailDestination
import com.github.whitescent.mastify.screen.destinations.StatusMediaScreenDestination
import com.github.whitescent.mastify.ui.component.AppHorizontalDivider
import com.github.whitescent.mastify.ui.component.CenterRow
import com.github.whitescent.mastify.ui.component.StatusAppendingIndicator
import com.github.whitescent.mastify.ui.component.StatusEndIndicator
import com.github.whitescent.mastify.ui.component.WidthSpacer
import com.github.whitescent.mastify.ui.component.drawVerticalScrollbar
import com.github.whitescent.mastify.ui.component.status.StatusListItem
import com.github.whitescent.mastify.ui.component.status.StatusSnackBar
import com.github.whitescent.mastify.ui.component.status.paging.EmptyStatusListPlaceholder
import com.github.whitescent.mastify.ui.component.status.paging.PageType
import com.github.whitescent.mastify.ui.component.status.paging.StatusListLoadError
import com.github.whitescent.mastify.ui.component.status.paging.StatusListLoading
import com.github.whitescent.mastify.ui.component.status.rememberStatusSnackBarState
import com.github.whitescent.mastify.ui.theme.AppTheme
import com.github.whitescent.mastify.ui.transitions.BottomBarScreenTransitions
import com.github.whitescent.mastify.utils.AppState
import com.github.whitescent.mastify.viewModel.HomeViewModel
import com.github.whitescent.mastify.viewModel.TimelinePosition
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import logcat.logcat

@OptIn(ExperimentalMaterialApi::class, FlowPreview::class)
@AppNavGraph(start = true)
@Destination(style = BottomBarScreenTransitions::class)
@Composable
fun Home(
  appState: AppState,
  drawerState: DrawerState,
  timeline: ImmutableList<StatusUiData>,
  timelinePosition: TimelinePosition,
  navigator: DestinationsNavigator,
  viewModel: HomeViewModel = hiltViewModel()
) {
  val lazyState = rememberLazyListState(
    initialFirstVisibleItemIndex = timelinePosition.index,
    initialFirstVisibleItemScrollOffset = timelinePosition.offset
  )
  val firstVisibleIndex by remember {
    derivedStateOf {
      lazyState.firstVisibleItemIndex
    }
  }
  // val timeline by viewModel.timelineListStateFlow.collectAsStateWithLifecycle()
  val avatar by viewModel.currentAccountAvatar.collectAsStateWithLifecycle()

  var refreshing by remember { mutableStateOf(false) }

  val scope = rememberCoroutineScope()
  val snackbarState = rememberStatusSnackBarState()
  val uiState = viewModel.uiState
  val context = LocalContext.current
  val pullRefreshState = rememberPullRefreshState(
    refreshing = refreshing,
    onRefresh = {
      scope.launch {
        refreshing = true
        delay(500)
        viewModel.refreshTimeline()
        refreshing = false
      }
    },
  )
  Box(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .padding(bottom = appState.appPaddingValues.calculateBottomPadding())
      .pullRefresh(pullRefreshState)
  ) {
    Column {
      HomeTopBar(
        avatar = avatar,
        openDrawer = {
          scope.launch {
            drawerState.open()
          }
        },
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
      )
      HorizontalDivider(thickness = 0.5.dp, color = AppTheme.colors.divider)
      logcat { "loadState ${uiState.timelineLoadState} size ${timeline.size}" }
      when (timeline.size) {
        0 -> {
          when {
            uiState.timelineLoadState == LoadState.Error -> StatusListLoadError { viewModel.refreshTimeline() }
            uiState.timelineLoadState == LoadState.NotLoading && uiState.endReached ->
              EmptyStatusListPlaceholder(
                pageType = PageType.Timeline,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
              )
            else -> StatusListLoading(Modifier.fillMaxSize())
          }
        }
        else -> {
          Box {
            LazyColumn(
              state = lazyState,
              modifier = Modifier.fillMaxSize().drawVerticalScrollbar(lazyState)
            ) {
              itemsIndexed(
                items = timeline,
                contentType = { _, _ -> StatusUiData },
                key = { _, item -> item.id }
              ) { index, status ->
                val replyChainType by remember(status, timeline.size, index) {
                  mutableStateOf(timeline.getReplyChainType(index))
                }
                val hasUnloadedParent by remember(status, timeline.size, index) {
                  mutableStateOf(timeline.hasUnloadedParent(index))
                }
                StatusListItem(
                  status = status,
                  replyChainType = replyChainType,
                  hasUnloadedParent = hasUnloadedParent,
                  action = {
                    viewModel.onStatusAction(it, context, status.actionable)
                  },
                  navigateToDetail = {
                    navigator.navigate(
                      StatusDetailDestination(
                        status = status.actionable,
                        originStatusId = status.id
                      )
                    )
                  },
                  navigateToMedia = { attachments, targetIndex ->
                    navigator.navigate(
                      StatusMediaScreenDestination(attachments.toTypedArray(), targetIndex)
                    )
                  },
                  navigateToProfile = {
                    navigator.navigate(ProfileDestination(it))
                  }
                )
                if (!status.hasUnloadedStatus && (replyChainType == End || replyChainType == Null))
                  AppHorizontalDivider()
                if (status.hasUnloadedStatus)
                  LoadMorePlaceHolder { viewModel.loadUnloadedStatus(status.id) }
              }
              item {
                when (uiState.timelineLoadState) {
                  LoadState.Append -> StatusAppendingIndicator()
                  LoadState.Error -> {
                    // TODO Localization
                    Toast.makeText(context, "获取嘟文失败，请稍后重试", Toast.LENGTH_SHORT).show()
                    viewModel.append() // retry
                  }
                  else -> Unit
                }
                if (uiState.endReached) StatusEndIndicator(Modifier.padding(36.dp))
              }
            }
            NewStatusToast(
              visible = uiState.showNewStatusButton,
              count = uiState.newStatusCount,
              limitExceeded = uiState.needSecondLoad,
              modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
            ) {
              scope.launch {
                lazyState.scrollToItem(0)
                viewModel.dismissButton()
              }
            }
            Column(Modifier.align(Alignment.BottomEnd)) {
              Image(
                painter = painterResource(id = R.drawable.edit),
                contentDescription = null,
                modifier = Modifier
                  .padding(24.dp)
                  .align(Alignment.End)
                  .shadow(6.dp, CircleShape)
                  .background(AppTheme.colors.primaryGradient, CircleShape)
                  .clickable { navigator.navigate(PostDestination) }
                  .padding(16.dp)
              )
              StatusSnackBar(
                snackbarState = snackbarState,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 36.dp)
              )
            }
          }
        }
      }
    }
    PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))

    LaunchedEffect(Unit) {
      launch {
        viewModel.snackBarFlow.collect {
          snackbarState.show(it)
        }
      }
      launch {
        appState.scrollToTopFlow.collect {
          lazyState.scrollToItem(0)
        }
      }
      viewModel.fetchLatestAccountData()
      viewModel.refreshTimeline()
    }

    LaunchedEffect(firstVisibleIndex) {
      if (firstVisibleIndex == 0 && uiState.showNewStatusButton) viewModel.dismissButton()
      launch {
        snapshotFlow { firstVisibleIndex }
          .filter { timeline.isNotEmpty() }
          .map {
            !uiState.endReached && uiState.timelineLoadState != LoadState.Append &&
              lazyState.firstVisibleItemIndex >= (timeline.size - ((timeline.size / FETCHNUMBER) * PAGINGTHRESHOLD))
          }
          .filter { it }
          .collect {
            viewModel.append()
          }
      }
      launch {
        snapshotFlow { firstVisibleIndex }
          .debounce(500L)
          .filter { firstVisibleIndex != timelinePosition.index }
          .collectLatest {
            viewModel.updateTimelinePosition(it, lazyState.firstVisibleItemScrollOffset)
          }
      }
    }
  }
}

@Composable
private fun NewStatusToast(
  visible: Boolean,
  count: Int,
  limitExceeded: Boolean,
  modifier: Modifier = Modifier,
  onDismiss: () -> Unit,
) {
  AnimatedVisibility(
    visible = visible,
    enter = slideInVertically(tween(300)) + fadeIn(),
    exit = scaleOut(targetScale = 0.8f) +
      slideOutVertically(tween(300)) { -it } + fadeOut(),
    modifier = modifier
  ) {
    Surface(
      shape = CircleShape,
      color = AppTheme.colors.accent,
      shadowElevation = 4.dp
    ) {
      CenterRow(
        modifier = Modifier
          .clickable { onDismiss() }
          .padding(horizontal = 18.dp, vertical = 8.dp),
      ) {
        Icon(
          painter = painterResource(id = R.drawable.arrow_up),
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
        WidthSpacer(value = 4.dp)
        Text(
          text = when (limitExceeded) {
            true -> stringResource(id = R.string.many_posts_title)
            else -> pluralStringResource(id = R.plurals.new_post, count, count)
          },
          fontSize = 16.sp,
          color = Color.White,
          fontWeight = FontWeight(500),
        )
      }
    }
  }
}

@Composable
private fun LoadMorePlaceHolder(loadMore: () -> Unit) {
  var loading by remember { mutableStateOf(false) }
  Column {
    Surface(
      modifier = Modifier
        .padding(24.dp)
        .fillMaxWidth()
        .height(56.dp),
      shape = RoundedCornerShape(18.dp),
      color = Color(0xFFebf4fb)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clickable {
            loading = true
            loadMore()
          }
          .padding(12.dp),
        contentAlignment = Alignment.Center
      ) {
        Crossfade(loading) {
          when (it) {
            false -> {
              Text(
                text = stringResource(id = R.string.load_more_title),
                color = AppTheme.colors.hintText,
                fontSize = 16.sp,
              )
            }
            true -> CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = AppTheme.colors.primaryContent,
              strokeWidth = 1.5.dp
            )
          }
        }
      }
    }
  }
}
