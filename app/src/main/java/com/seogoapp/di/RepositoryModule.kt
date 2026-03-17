package com.seogoapp.di

import com.seogoapp.data.repository.FolderRepository
import com.seogoapp.data.repository.SceneRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * FolderRepository, SceneRepository 는 @Singleton + @Inject constructor 로
 * 자동 바인딩되므로 별도 @Provides 없이 Hilt가 처리합니다.
 * 인터페이스 추상화가 필요해질 경우 여기에 binds를 추가하세요.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule
