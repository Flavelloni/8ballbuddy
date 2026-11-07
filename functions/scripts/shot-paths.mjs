// File paths may change, but challenge IDs and shared ?shot= links must remain stable.
export function shotIdForPath(relativePath, explicitId) {
  const parts = relativePath.split('/');
  const id = explicitId ?? (parts.length === 4 ? `${parts[1]}/${parts[3]}` : relativePath);
  if (!/^[A-Za-z0-9_-]{11}\/\d{6}(?:_\d+)?$/.test(id)) {
    throw new Error(`Invalid shot ID or folder structure: ${relativePath}`);
  }
  return id;
}

export function ignoredShotDirectory(name) {
  return name.startsWith('.') || name.endsWith('_bad');
}
