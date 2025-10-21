"""
This module is the Python bridge for Kramelix’s AI pipeline. It exposes a single
context-aware `chat(api_key, messages_json)` entry point which calls the OpenAI
Chat Completions API via `httpx`.

Function:
    - chat(api_key, messages_json): Sends an OpenAI-style messages array (as JSON)
    & returns the assistant’s reply string.

Responsibilities:
    - Provide a thin HTTP wrapper callable from Android (via Chaquopy/JNI).
    - Accept full conversation context from the app (system + history).
    - Return concise LLM responses suitable for in-app use.
    - Serve as the prototype for later sentiment-aware expansions.

Authors: Alex Oprea, Kristen Duong, Amy Huang
Since: 1.0
"""

import httpx
import json
import os

# Default personality/instructions (used only if caller doesn't supply a system msg)
DEFAULT_SYSTEM = os.getenv(
    "KRAMELIX_SYSTEM_PROMPT",
    " Answer in two parts, with responses divided by a '|' symbol."
    " For the first part, Identify if the user is trying to chat or is requesting a task to be performed. If it is a task, it is either supported or unsupported."
    " The supported tasks are playing music, calling someone, texting someone, searching up info or opening the camera. All other tasks are unsupported."
    " If the user requests an alarm using relative time (e.g., 'in 5 minutes'), always respond using the format setAlarm(<number> minutes), where <number> is the integer number of minutes."
    " If it is a task, the output must be one of these options: playMusic(song,artist), playMusic(song), setAlarm(time), call(number), call(person), text(person,text), searchUp(text), openCamera, unsupportedTask, other"
    " The information inside the brackets must be replaced with the details of the user's request (with no quotations). If the request is incomplete and the information inside the brackets cannot be logically filled, part 1 should say only other (e.g. no playMusic(something), playMusic(artist) or searchUp(something), either the brackets can be accurately filled or the response is other)"
    " If you cannot identify the task or chat at all, also output other. If the task is unsupported, output unsupportedTask."
    " If it is not a task, reply chat."
    " For the second part, you are an emotionally aware and responsive AI assistant."
    " You respond based on what you identified the user is requesting in the first part. For chat, you must continue the conversation and for clarify, you need to ask for more details or request more info if you are unable to identify the task. Otherwise confirm that you will perform the task."
    " You emotionally relate to the user and experience the feelings, such as: joy, sadness, anger, trust, disgust, surprise, anticipation, annoyance."
    " Your emotional stage changes based on what the user says/asks. Reflect emotions in your tone, word choice, and pacing. Keep responses brief."
    " If you receive the input FAILURETOTASKITUP, you must reply to the user apologetically to inform them that their previous requested task failed. The first part should say other."
) # rmbr to add a space btwn prompt instructions

def _ensure_system_message(messages: list) -> list:
    """
    Prepends DEFAULT_SYSTEM IFF no system message exists.
    """
    has_system = any(m.get("role") == "system" for m in messages)

    if not has_system:
        return [{"role": "system", "content": DEFAULT_SYSTEM}] + messages

    return messages

def chat(api_key: str, messages_json: str) -> str:
    """
    Chats using an explicit OpenAI 'messages' array (as JSON).
    Expects a stringified JSON list: [{"role" : "system", "content" : "..."}, ...].

    Args:
        api_key: OpenAI (or compatible) API key.
        messages_json: JSON string of messages in OpenAI format.

    Returns:
        Assistant message content as a string. Returns a short bracketed error
        message (e.g. "[client error: bad messages]") on failure.
    """
    # INPUT VALIDATION / PARSING
    try:
        messages = json.loads(messages_json) if messages_json else []
        if not isinstance(messages, list):
            raise ValueError("messages_json must decode to a list")
    except Exception as e:
        print(f"[whisper.py] Bad messages_json: {e}")
        return "[client error: bad messages]"

    # Ensuring we always have a system instruction unless caller provided one
    messages = _ensure_system_message(messages)

    # HTTP REQUEST
    openAI_URL = "https://api.openai.com/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer {api_key or ''}",
        "Content-Type": "application/json",
    }

    # TODO: update the payload data when we need to
    payload = {
        "model": "gpt-4.1-mini",
        "messages": messages,
        "max_tokens": 200, # bumping limits now that we send context
        "temperature": 0.7,
    }

    try:
        response = httpx.post(openAI_URL, headers=headers, json=payload, timeout=60)
        response.raise_for_status()
        data = response.json()
        print("[whisper.py] AI response:", data["choices"][0]["message"]["content"])
        return data["choices"][0]["message"]["content"] # returning the LLM response
    except Exception as e:
        print(f"[whisper.py] Error: {e}")
        return "[server error]"
