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

package com.github.whitescent.mastify.data.repository

import at.connyduck.calladapter.networkresult.fold
import com.github.whitescent.mastify.network.MastodonApi
import com.github.whitescent.mastify.network.model.search.SearchResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExploreRepository @Inject constructor(
  private val api: MastodonApi
) {
  suspend fun getPreviewResultsForSearch(keyword: String): SearchPreviewResult {
    if (keyword.isBlank()) return SearchPreviewResult.Success(null)
    return api.searchSync(query = keyword, limit = 10).fold(
      {
        SearchPreviewResult.Success(it)
      },
      {
        it.printStackTrace()
        SearchPreviewResult.Failure(it)
      }
    )
  }
}

sealed interface SearchPreviewResult {
  data class Success(val response: SearchResult?) : SearchPreviewResult
  data class Failure(val exception: Throwable) : SearchPreviewResult
}