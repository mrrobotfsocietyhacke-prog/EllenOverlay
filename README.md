# EllenOverlay — Android project

Полный проект Android-приложения с плавающим персонажем, чатом, TTS и анимацией PNG-спрайтов.

## Что внутри

- Floating overlay поверх других приложений.
- Перетаскивание Эллен пальцем.
- Сохранение позиции и размера.
- Idle-анимация: `idle_01..idle_05`.
- Периодическое моргание: `blink_01..blink_04`.
- Анимация рта во время TTS: `talk_01..talk_06`.
- Эмоции: `happy`, `angry`, `sleepy`.
- OpenAI-compatible API: можно указать свой Base URL и модель.
- Русский Android Text-to-Speech.
- GitHub Actions для сборки APK без компьютера.

## Где спрайты

`app/src/main/assets/sprites/`

## Сборка через GitHub с телефона

1. Создай новый GitHub repository.
2. Распакуй этот ZIP.
3. Загрузи содержимое папки `EllenOverlay_FULL` в repository.
4. Открой вкладку **Actions**.
5. Выбери **Build EllenOverlay APK**.
6. Нажми **Run workflow**.
7. После завершения открой запуск workflow и скачай artifact `EllenOverlay-debug`.
8. Внутри artifact будет `app-debug.apk`.

## Настройка приложения

При первом запуске:

1. Выдай разрешение **Показывать поверх других приложений**.
2. Нажми **ЗАПУСТИТЬ OVERLAY**.
3. Открой **НАСТРОЙКИ / OVERLAY**.
4. Укажи API KEY, Base URL и MODEL.

По умолчанию Base URL: `https://api.openai.com/v1`.

Приложение использует стандартный OpenAI-compatible endpoint `/chat/completions`, поэтому Base URL можно заменить на совместимый сервис.

## Важно

PNG находятся в `assets`, а не в `res/drawable`, потому что так удобнее хранить наборы кадров по папкам.
