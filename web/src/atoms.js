import { atom } from 'recoil';

export const atoms = {
    status: atom({ key: 'status', default: { open: false }}),
};
