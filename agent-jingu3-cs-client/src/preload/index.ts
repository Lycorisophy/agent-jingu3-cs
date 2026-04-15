import { contextBridge } from 'electron';

/**
 * 首版不暴露 Node 能力；后续可白名单暴露版本号等。
 */
contextBridge.exposeInMainWorld('jingu3', {
  platform: process.platform,
});
