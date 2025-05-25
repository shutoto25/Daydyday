package com.gmail.shu10.dev.app.domain

import android.content.Context
import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * TrimVideoUseCaseの単体テスト
 */
class TrimVideoUseCaseTest {

    private lateinit var repository: IVideoEditorRepository
    private lateinit var useCase: TrimVideoUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = TrimVideoUseCase(repository)
    }

    @Test
    fun `正常に動画をトリミングできる`() = runTest {
        // Given
        val context = mockk<Context>()
        val inputUri = mockk<Uri>()
        val outputFile = mockk<File>()
        val startTimeUs = 1000L
        val durationUs = 2000L

        coEvery {
            repository.trimVideo(context, inputUri, outputFile, startTimeUs, durationUs)
        } returns true

        // When
        val result = useCase(context, inputUri, outputFile, startTimeUs, durationUs)

        // Then
        assertTrue(result)
        coVerify(exactly = 1) {
            repository.trimVideo(context, inputUri, outputFile, startTimeUs, durationUs)
        }
    }

    @Test
    fun `動画トリミングに失敗した場合はfalseを返す`() = runTest {
        // Given
        val context = mockk<Context>()
        val inputUri = mockk<Uri>()
        val outputFile = mockk<File>()
        val startTimeUs = 1000L
        val durationUs = 2000L

        coEvery {
            repository.trimVideo(context, inputUri, outputFile, startTimeUs, durationUs)
        } returns false

        // When
        val result = useCase(context, inputUri, outputFile, startTimeUs, durationUs)

        // Then
        assertFalse(result)
    }

    @Test
    fun `デフォルト値で動画をトリミングできる`() = runTest {
        // Given
        val context = mockk<Context>()
        val inputUri = mockk<Uri>()
        val outputFile = mockk<File>()
        val startTimeUs = 1000L

        coEvery {
            repository.trimVideo(context, inputUri, outputFile, startTimeUs, 1_000_000L)
        } returns true

        // When
        val result = useCase(context, inputUri, outputFile, startTimeUs)

        // Then
        assertTrue(result)
        coVerify(exactly = 1) {
            repository.trimVideo(context, inputUri, outputFile, startTimeUs, 1_000_000L)
        }
    }
}
