# Image Master

ActivityMaster module that stores, secures and serves images as **resource items** through GuicedEE/Vert.x web.

Images inherit the full warehouse security matrix (default / scope-restricted), ActiveFlag lifecycle
and token propagation. Retrieval supports on-the-fly width/height bounded scaling for optimum payloads.

## System

| Item | Value |
|------|-------|
| System name | `Image System` |
| Resource item type | `Image` |
| Install order | `ImageSystemInstall` (`sortOrder = 1100`) — creates the `Image` type only |

## REST API

Base path: `{enterprise}/image` — secured via `SessionUtils.withActivityMaster`.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `{requestingSystemName}/upload?name=` | Stores image bytes, returns the resource item id |
| `GET`  | `{requestingSystemName}/{imageId}?w=&h=` | Returns the image; optional bounded scaling; long-lived cache headers |

## Event bus

| Address | Body | Reply |
|---------|------|-------|
| `image.store` | image bytes | created resource item id |

## Service

`IImageService` — `toBufferedImage` / `toBytes` / `optimize` (Scalr ULTRA_QUALITY), plus
`storeImage` / `getImage` / `getOptimizedImage` backed by `IResourceItemService` with security tokens.

