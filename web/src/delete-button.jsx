import { useState } from 'react';
import LoadingButton from '@mui/lab/LoadingButton';
import Button from '@mui/material/Button';
import Modal from '@mui/material/Modal';
import Box from '@mui/material/Box';
import Api from './api';

const style = {
    position: 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: 400,
    bgcolor: 'background.paper',
    border: '2px solid #000',
    boxShadow: 24,
    p: 4,
};

const DeleteButton = ({ id, setData }) => {
    const [ modalOpen, setModalOpen ] = useState(false);
    const [ loading, setLoading ] = useState(false);
    const { deleteTx } = Api();

    const confirmDelete = () => setModalOpen(true);
    const dismiss = () => setModalOpen(false);

    const performDelete = () => {
        setLoading(true);
        deleteTx(id, () => {
            dismiss();
            setData((data) => data.filter(d => d.id !== id));
        });
    };

    return (
        <>
            <Button
                color="error"
                variant="outlined"
                onClick={confirmDelete}
            >
                Delete
            </Button>
            <Modal
                open={modalOpen}
                onClose={dismiss}
            >
                <Box sx={style}>
                    Confirm delete? This change is permanent.
                    <Box sx={{ display: 'flex', gap: '1rem', p: 1 }}>
                        <LoadingButton
                            variant="contained"
                            color="error"
                            loading={loading}
                            onClick={performDelete}
                        >
                            Delete
                        </LoadingButton>
                        <Button variant="outlined" onClick={dismiss}>Cancel</Button>
                    </Box>
                </Box>
            </Modal>
        </>
    );
};
export default DeleteButton;
