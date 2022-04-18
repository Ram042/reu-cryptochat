@echo off
call gradlew.bat :cli:run --q --console=plain --args="%*"
@echo on
