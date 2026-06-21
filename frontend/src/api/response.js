function hasOwn(value, key) {
  return Boolean(value) && Object.prototype.hasOwnProperty.call(value, key)
}

function ensureNoLogicalError(payload, fallbackMessage) {
  if (payload?.success === false) {
    throw new Error(payload?.error || fallbackMessage)
  }
}

export function unwrapApiPayload(payload, fallbackMessage) {
  ensureNoLogicalError(payload, fallbackMessage)

  if (hasOwn(payload, 'data')) {
    return payload.data
  }

  return payload
}

export function unwrapApiField(payload, fieldName, fallbackValue, fallbackMessage) {
  const unwrapped = unwrapApiPayload(payload, fallbackMessage)

  if (hasOwn(unwrapped, fieldName)) {
    return unwrapped[fieldName]
  }

  return unwrapped ?? fallbackValue
}
