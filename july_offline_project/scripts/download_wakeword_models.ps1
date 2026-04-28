$assetsDir = "app/src/main/assets"
if (!(Test-Path $assetsDir)) {
    New-Item -ItemType Directory -Force -Path $assetsDir
}

Write-Host "Downloading wakeword_embedding.onnx..."
Invoke-WebRequest -Uri "https://github.com/dscripka/openWakeWord/releases/download/v0.5.1/embedding_model.onnx" -OutFile "$assetsDir/wakeword_embedding.onnx"

Write-Host "Downloading wakeword_model.onnx (hey_jarvis as placeholder)..."
Invoke-WebRequest -Uri "https://github.com/dscripka/openWakeWord/releases/download/v0.5.1/hey_jarvis_v0.1.onnx" -OutFile "$assetsDir/wakeword_model.onnx"

Write-Host "Done."
